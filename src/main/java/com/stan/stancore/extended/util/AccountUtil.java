package com.stan.stancore.extended.util;

import com.systemspecs.remita.dto.account.AccountDebitRequest;
import com.systemspecs.remita.dto.auth.MerchantDetailsDto;
import com.systemspecs.remita.dto.auth.SubscribedServiceAccountDetailsDTO;
import com.systemspecs.remita.enumeration.AccountType;
import com.systemspecs.remita.enumeration.CoreSDKResult;
import com.systemspecs.remita.enumeration.ServicesEnum;
import com.systemspecs.remita.enumeration.VendProcessor;
import com.systemspecs.remita.sdk.auth.CoreSDKAuth;
import com.systemspecs.remita.sdk.debit.CoreSdkAccount;
import com.systemspecs.remita.vending.extended.exception.BadRequestException;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.vendingcommon.dto.request.TransactionRequest;
import com.systemspecs.remita.vending.vendingcommon.entity.FundRecoup;
import com.systemspecs.remita.vending.vendingcommon.entity.Transaction;
import com.systemspecs.remita.vending.vendingcommon.enums.Action;
import com.systemspecs.remita.vending.vendingcommon.enums.DebitStatus;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import com.systemspecs.remita.vending.vendingcommon.eventsticketing.dto.request.CompleteEventBookingRequest;
import com.systemspecs.remita.vending.vendingcommon.flight.dto.request.BookFlightRequest;
import com.systemspecs.remita.vending.vendingcommon.movies.dto.request.CompleteMovieBookingRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Data
@Component
public class AccountUtil {

    private final CoreSdkAccount coreSdkAccount;
    private final CoreSDKAuth coreSDKAuth;

    public AccountDebitRequest getAccountDebitRequest(MerchantDetailsDto merchantDetailsDto, BigDecimal amount, String clientReference) {
        AccountDebitRequest accountDebitRequest = new AccountDebitRequest();

        boolean requiresServiceAccount = SubscriptionUtil.getRequiresServiceAccount(merchantDetailsDto);
        log.info("requiresServiceAccount on Merchant is => {}", requiresServiceAccount);
        if (!requiresServiceAccount) {
            log.info("Enter because service account is NOT required, Main account is required");
            if (StringUtils.isBlank(merchantDetailsDto.getAccountNumber()) || StringUtils.isBlank(merchantDetailsDto.getBankCode())) {
                throw new NotFoundException("Vending service account details not available ");
            }
            accountDebitRequest.setAccountNumber(merchantDetailsDto.getAccountNumber());
            accountDebitRequest.setBankCode(merchantDetailsDto.getBankCode());
        } else {
            log.info("Enter because service account is required.");
            Optional<SubscribedServiceAccountDetailsDTO> accountDetailsDTO = coreSDKAuth.getSubscribedServiceAccount(
                merchantDetailsDto,
                ServicesEnum.VENDING
            );

            if (accountDetailsDTO.isEmpty()) {
                throw new NotFoundException("Vending service account details not available ");
            }
            merchantDetailsDto.setAccountNumber(accountDetailsDTO.get().getAccountNumber());
            merchantDetailsDto.setBankCode(accountDetailsDTO.get().getBankCode());
            accountDebitRequest.setAccountNumber(accountDetailsDTO.get().getAccountNumber());
            accountDebitRequest.setBankCode(accountDetailsDTO.get().getBankCode());
        }
        accountDebitRequest.setAccountType(AccountType.PREPAID);
        accountDebitRequest.setAmount(amount);
        accountDebitRequest.setDebitRef(clientReference);
        accountDebitRequest.setServiceName("VENDING");
        accountDebitRequest.setOrgId(merchantDetailsDto.getOrgId());
        return accountDebitRequest;
    }

    public void holdOriginalTransactionAmount(AccountDebitRequest accountDebitRequest) {
        CoreSDKResult holdResult = coreSdkAccount.holdAmount(accountDebitRequest);
        if (holdResult == CoreSDKResult.INSUFFICIENT_FUNDS) {
            throw new BadRequestException(TransactionStatus.INSUFFICIENT_FUND.getMessage(), TransactionStatus.INSUFFICIENT_FUND.getCode());
        } else if (holdResult == CoreSDKResult.FAILED) {
            throw new BadRequestException("Transaction Failed", TransactionStatus.TRANSACTION_FAILED.getCode());
        }
    }

    public CoreSDKResult releaseAndDebitFund(AccountDebitRequest accountDebitRequest, VendProcessor processor) {
        CoreSDKResult releaseAndDebitFundResult = coreSdkAccount.doVendingPrepaidReleaseDebit(accountDebitRequest, processor);
        return releaseAndDebitFundResult;
    }

    public CoreSDKResult releaseHeldFund(AccountDebitRequest accountDebitRequest) {
        CoreSDKResult releaseFundResult = coreSdkAccount.releaseAmount(accountDebitRequest);
        return releaseFundResult;
    }

    public CoreSDKResult debitFund(AccountDebitRequest accountDebitRequest, VendProcessor processor) {
        CoreSDKResult debitFundResult = coreSdkAccount.doPrepaidVendingDebit(accountDebitRequest, processor);
        return debitFundResult;
    }

    public AccountDebitRequest getQueryAccountDebitRequest(Transaction transaction) {
        AccountDebitRequest accountDebitRequest = new AccountDebitRequest();
        accountDebitRequest.setAccountNumber(transaction.getAccountNumber());
        accountDebitRequest.setBankCode(transaction.getBankCode());
        accountDebitRequest.setAccountType(AccountType.PREPAID);
        accountDebitRequest.setAmount(transaction.getDiscountedAmount());
        accountDebitRequest.setDebitRef(transaction.getClientReference());
        accountDebitRequest.setServiceName("VENDING");
        accountDebitRequest.setOrgId(transaction.getMerchantOrgId());
        return accountDebitRequest;
    }

    public AccountDebitRequest getQueryAccountReaseRequest(Transaction transaction) {
        AccountDebitRequest accountDebitRequest = new AccountDebitRequest();
        accountDebitRequest.setAccountNumber(transaction.getAccountNumber());
        accountDebitRequest.setBankCode(transaction.getBankCode());
        accountDebitRequest.setAccountType(AccountType.PREPAID);
        accountDebitRequest.setAmount(transaction.getAmount());
        accountDebitRequest.setDebitRef(transaction.getClientReference());
        accountDebitRequest.setServiceName("VENDING");
        accountDebitRequest.setOrgId(transaction.getMerchantOrgId());
        return accountDebitRequest;
    }

    public FundRecoup getReleasedFundRecoup(TransactionRequest request) {
        FundRecoup fundRecoup = new FundRecoup();
        fundRecoup.setReference(request.getClientReference());
        fundRecoup.setStatus(DebitStatus.OPEN);
        fundRecoup.setCreatedAt(new Date());
        fundRecoup.setAction(Action.DEBIT);
        return fundRecoup;
    }

    public FundRecoup getFailedFundRecoup(TransactionRequest request) {
        FundRecoup fundRecoup = new FundRecoup();
        fundRecoup.setReference(request.getClientReference());
        fundRecoup.setStatus(DebitStatus.OPEN);
        fundRecoup.setCreatedAt(new Date());
        fundRecoup.setAction(Action.RELEASED_AND_DEBIT);
        return fundRecoup;
    }

    public FundRecoup getReleasedFundRecoup(Transaction request) {
        FundRecoup fundRecoup = new FundRecoup();
        fundRecoup.setReference(request.getClientReference());
        fundRecoup.setStatus(DebitStatus.OPEN);
        fundRecoup.setCreatedAt(new Date());
        fundRecoup.setAction(Action.DEBIT);
        return fundRecoup;
    }

    public FundRecoup getFailedFundRecoup(Transaction request) {
        FundRecoup fundRecoup = new FundRecoup();
        fundRecoup.setReference(request.getClientReference());
        fundRecoup.setStatus(DebitStatus.OPEN);
        fundRecoup.setCreatedAt(new Date());
        fundRecoup.setAction(Action.RELEASED_AND_DEBIT);
        return fundRecoup;
    }

    public FundRecoup getReleasedFundRecoup(BookFlightRequest request) {
        FundRecoup fundRecoup = new FundRecoup();
        fundRecoup.setReference(request.getPaymentIdentifier());
        fundRecoup.setStatus(DebitStatus.OPEN);
        fundRecoup.setCreatedAt(new Date());
        fundRecoup.setAction(Action.DEBIT);
        return fundRecoup;
    }

    public FundRecoup getFailedFundRecoup(BookFlightRequest request) {
        FundRecoup fundRecoup = new FundRecoup();
        fundRecoup.setReference(request.getPaymentIdentifier());
        fundRecoup.setStatus(DebitStatus.OPEN);
        fundRecoup.setCreatedAt(new Date());
        fundRecoup.setAction(Action.RELEASED_AND_DEBIT);
        return fundRecoup;
    }

    public FundRecoup getHoldFundRecoup(TransactionRequest request) {
        FundRecoup fundRecoup = new FundRecoup();
        fundRecoup.setReference(request.getClientReference());
        fundRecoup.setStatus(DebitStatus.OPEN);
        fundRecoup.setCreatedAt(new Date());
        fundRecoup.setAction(Action.RELEASE);
        return fundRecoup;
    }

    public FundRecoup getHoldFundRecoup(Transaction request) {
        FundRecoup fundRecoup = new FundRecoup();
        fundRecoup.setReference(request.getClientReference());
        fundRecoup.setStatus(DebitStatus.OPEN);
        fundRecoup.setCreatedAt(new Date());
        fundRecoup.setAction(Action.RELEASE);
        return fundRecoup;
    }

    public FundRecoup getHoldFundRecoup(BookFlightRequest request) {
        FundRecoup fundRecoup = new FundRecoup();
        fundRecoup.setReference(request.getPaymentIdentifier());
        fundRecoup.setStatus(DebitStatus.OPEN);
        fundRecoup.setCreatedAt(new Date());
        fundRecoup.setAction(Action.RELEASE);
        return fundRecoup;
    }

    public FundRecoup getReleasedFundRecoup(CompleteEventBookingRequest request) {
        FundRecoup fundRecoup = new FundRecoup();
        fundRecoup.setReference(request.getPaymentIdentifier());
        fundRecoup.setStatus(DebitStatus.OPEN);
        fundRecoup.setCreatedAt(new Date());
        fundRecoup.setAction(Action.DEBIT);
        return fundRecoup;
    }

    public FundRecoup getFailedFundRecoup(CompleteEventBookingRequest request) {
        FundRecoup fundRecoup = new FundRecoup();
        fundRecoup.setReference(request.getPaymentIdentifier());
        fundRecoup.setStatus(DebitStatus.OPEN);
        fundRecoup.setCreatedAt(new Date());
        fundRecoup.setAction(Action.RELEASED_AND_DEBIT);
        return fundRecoup;
    }

    public FundRecoup getHoldFundRecoup(CompleteEventBookingRequest request) {
        FundRecoup fundRecoup = new FundRecoup();
        fundRecoup.setReference(request.getPaymentIdentifier());
        fundRecoup.setStatus(DebitStatus.OPEN);
        fundRecoup.setCreatedAt(new Date());
        fundRecoup.setAction(Action.RELEASE);
        return fundRecoup;
    }

    public FundRecoup getReleasedFundRecoup(CompleteMovieBookingRequest request) {
        FundRecoup fundRecoup = new FundRecoup();
        fundRecoup.setReference(request.getPaymentIdentifier());
        fundRecoup.setStatus(DebitStatus.OPEN);
        fundRecoup.setCreatedAt(new Date());
        fundRecoup.setAction(Action.DEBIT);
        return fundRecoup;
    }

    public FundRecoup getFailedFundRecoup(CompleteMovieBookingRequest request) {
        FundRecoup fundRecoup = new FundRecoup();
        fundRecoup.setReference(request.getPaymentIdentifier());
        fundRecoup.setStatus(DebitStatus.OPEN);
        fundRecoup.setCreatedAt(new Date());
        fundRecoup.setAction(Action.RELEASED_AND_DEBIT);
        return fundRecoup;
    }

    public FundRecoup getHoldFundRecoup(CompleteMovieBookingRequest request) {
        FundRecoup fundRecoup = new FundRecoup();
        fundRecoup.setReference(request.getPaymentIdentifier());
        fundRecoup.setStatus(DebitStatus.OPEN);
        fundRecoup.setCreatedAt(new Date());
        fundRecoup.setAction(Action.RELEASE);
        return fundRecoup;
    }
}
