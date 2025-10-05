package com.stan.stancore.extended.service;

import com.systemspecs.remita.dto.account.AccountDebitRequest;
import com.systemspecs.remita.dto.auth.MerchantDetailsDto;
import com.systemspecs.remita.dto.auth.SubscribedServiceAccountDetailsDTO;
import com.systemspecs.remita.enumeration.AccountType;
import com.systemspecs.remita.enumeration.CoreSDKResult;
import com.systemspecs.remita.enumeration.ServicesEnum;
import com.systemspecs.remita.enumeration.VendProcessor;
import com.systemspecs.remita.sdk.auth.CoreSDKAuth;
import com.systemspecs.remita.sdk.debit.CoreSdkAccount;
import com.systemspecs.remita.vending.extended.dto.CommissionDTO;
import com.systemspecs.remita.vending.extended.dto.PerformVendResponse;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.exception.UnknownException;
import com.systemspecs.remita.vending.extended.util.AccountUtil;
import com.systemspecs.remita.vending.extended.util.SubscriptionUtil;
import com.systemspecs.remita.vending.vendingcommon.dto.request.TransactionRequest;
import com.systemspecs.remita.vending.vendingcommon.entity.FundRecoup;
import com.systemspecs.remita.vending.vendingcommon.enums.SubscriptionType;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import com.systemspecs.remita.vending.vendingcommon.repository.FundRecoupRepository;
import com.systemspecs.remita.vending.vendingcommon.service.VendingServiceProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

import static com.systemspecs.remita.vending.extended.enums.ResponseStatus.INTERNAL_SERVER_ERROR;

@Service
@Slf4j
@RequiredArgsConstructor
public class DisburseFundsServiceJob {

    private final CoreSDKAuth coreSDKAuth;
    private final CoreSdkAccount coreSdkAccount;
    private final AccountUtil accountUtil;
    private final FundRecoupRepository fundRecoupRepository;
    private final VendingServiceProcessorService vendingServiceProcessorService;


    @Async("disBurseFundsTaskExecutor")
    public void disBurseFunds(
        final MerchantDetailsDto merchantDetailsDto,
        final CommissionDTO commission,
        final TransactionRequest request,
        final PerformVendResponse vendingResponse
    ) {
        log.info("Inside disBurseFunds (legacy)");
        // Legacy logic: only proceed if PREPAID
        if (!isPrepaid(merchantDetailsDto)) {
            log.debug("Skip disbursement: subscription is not PREPAID");
            return;
        }

        handleDisbursement(merchantDetailsDto, commission, request, vendingResponse);
    }

    @Async("disBurseFundsTaskExecutor")
    public void disBurseFundsV2(
        final MerchantDetailsDto merchantDetailsDto,
        final CommissionDTO commission,
        final TransactionRequest request,
        final PerformVendResponse vendingResponse,
        final String billingType
    ) {
        log.info("Inside disBurseFundsV2 with billingType: {}", billingType);

        // New logic: billingType check takes precedence
        if (billingType != null && !billingType.trim().isEmpty()) {
            if (!SubscriptionType.PREPAID.name().equalsIgnoreCase(billingType)) {
                log.debug("Skip disbursement: billingType [{}] is not PREPAID", billingType);
                return;
            }
        }

        handleDisbursement(merchantDetailsDto, commission, request, vendingResponse);
    }

    /**
     * Shared logic for handling SUCCESS / FAILED transaction statuses.
     */
    private void handleDisbursement(
        final MerchantDetailsDto merchantDetailsDto,
        final CommissionDTO commission,
        final TransactionRequest request,
        final PerformVendResponse vendingResponse
    ) {
        log.info("Handling disbursement for transactionStatus: {}", vendingResponse.getTransactionStatus());
        final TransactionStatus status = parseStatus(vendingResponse.getTransactionStatus());

        switch (status) {
            case TRANSACTION_FAILED:
                handleFailedVend(merchantDetailsDto, request);
                break;

            case SUCCESS:
                handleSuccessfulVend(merchantDetailsDto, commission, request, vendingResponse);
                break;

            default:
                log.debug("No disbursement action for txn status: {}", status);
        }
    }


    private void handleFailedVend(final MerchantDetailsDto merchantDetailsDto, final TransactionRequest request) {
        // Attempt to release held funds; if release fails, record for recoup
        final AccountDebitRequest releaseReq = buildAccountDebitRequest(
            merchantDetailsDto, request.getAmount(), request.getClientReference()
        );
        final CoreSDKResult result = coreSdkAccount.releaseAmount(releaseReq);
        log.info("Release held fund result: {} (clientRef={})", result, request.getClientReference());

        if (result != CoreSDKResult.RELEASED) {
            persistRecoup(accountUtil::getHoldFundRecoup, request);
        }
    }

    private void handleSuccessfulVend(
        final MerchantDetailsDto merchantDetailsDto,
        final CommissionDTO commission,
        final TransactionRequest request,
        final PerformVendResponse vendingResponse
    ) {
        final AccountDebitRequest debitReq = buildAccountDebitRequest(
            merchantDetailsDto, commission.getDiscountedAmount(), request.getClientReference()
        );

        final VendProcessor processor = parseProcessor(resolveProcessor(vendingResponse, request));

        // Release previously held funds and debit the discounted amount atomically (SDK‑level op)
        final CoreSDKResult result = coreSdkAccount.doVendingPrepaidReleaseDebit(debitReq, processor);
        log.info("Release+Debit result: {} (clientRef={}, processor={})", result, request.getClientReference(), processor);

        if (result == CoreSDKResult.RELEASED) {
            persistRecoup(accountUtil::getReleasedFundRecoup, request);
        } else if (result == CoreSDKResult.FAILED) {
            persistRecoup(accountUtil::getFailedFundRecoup, request);
        }
    }

    private boolean isPrepaid(final MerchantDetailsDto merchantDetailsDto) {
        final String subscriptionType = SubscriptionUtil.getSubscriptionType(merchantDetailsDto);
        return SubscriptionType.PREPAID.name().equalsIgnoreCase(subscriptionType);
    }

    private TransactionStatus parseStatus(final String status) {
        if (status == null)
            return TransactionStatus.UNKNOWN_CODE;
        try {
            return TransactionStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown transaction status: {}", status);
            return TransactionStatus.UNKNOWN_CODE;
        }
    }

    private VendProcessor parseProcessor(final String processor) {
        try {
            return VendProcessor.valueOf(processor.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }
    }

    private void persistRecoup(final java.util.function.Function<TransactionRequest, FundRecoup> supplier,
                               final TransactionRequest request) {
        final FundRecoup recoup = supplier.apply(request);
        fundRecoupRepository.save(recoup);
    }

    /**
     * Builds the debit/release request without mutating the incoming MerchantDetailsDto.
     */
    private AccountDebitRequest buildAccountDebitRequest(
        final MerchantDetailsDto merchantDetailsDto,
        final BigDecimal amount,
        final String clientReference
    ) {
        final AccountDebitRequest req = new AccountDebitRequest();

        final boolean requiresServiceAccount = SubscriptionUtil.getRequiresServiceAccount(merchantDetailsDto);
        final String accountNumber;
        final String bankCode;

        if (requiresServiceAccount) {
            log.debug("Using service account for VENDING");
            final Optional<SubscribedServiceAccountDetailsDTO> svcAccOpt =
                coreSDKAuth.getSubscribedServiceAccount(merchantDetailsDto, ServicesEnum.VENDING);

            if (svcAccOpt.isEmpty()) {
                throw new NotFoundException("Vending service account details not available ");
            }
            final SubscribedServiceAccountDetailsDTO svcAcc = svcAccOpt.get();
            accountNumber = svcAcc.getAccountNumber();
            bankCode = svcAcc.getBankCode();
        } else {
            log.debug("Using main merchant account");
            if (StringUtils.isBlank(merchantDetailsDto.getAccountNumber()) ||
                StringUtils.isBlank(merchantDetailsDto.getBankCode())) {
                throw new NotFoundException("Vending service account details not available ");
            }
            accountNumber = merchantDetailsDto.getAccountNumber();
            bankCode = merchantDetailsDto.getBankCode();
        }

        req.setAccountNumber(accountNumber);
        req.setBankCode(bankCode);
        req.setAccountType(AccountType.PREPAID);
        req.setAmount(amount);
        req.setDebitRef(clientReference);
        req.setServiceName("VENDING");
        req.setOrgId(merchantDetailsDto.getOrgId());
        return req;
    }

    /**
     * Derives the processor id from response or request and validates it.
     */
    public String resolveProcessor(final PerformVendResponse vendingResponse, final TransactionRequest request) {
        final String vendProcessor = vendingResponse.isProcessedWithFallback()
            ? vendingResponse.getFallbackProcessor()
            : getProcessorId(request);
        log.debug("Resolved vendProcessor: {}", vendProcessor);
        return vendProcessor;
    }

    private String getProcessorId(final TransactionRequest request) {
        final String productCode = request.getProductCode();
        log.debug(">>> Resolving processorId for productCode: {}", productCode);
        final String processorId = vendingServiceProcessorService.getProcessorId(productCode);
        if (processorId == null) {
            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }
        return processorId;
    }
}
