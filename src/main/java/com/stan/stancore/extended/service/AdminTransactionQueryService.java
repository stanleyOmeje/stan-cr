package com.stan.stancore.extended.service;

import com.systemspecs.remita.dto.account.AccountDebitRequest;
import com.systemspecs.remita.enumeration.CoreSDKResult;
import com.systemspecs.remita.enumeration.VendProcessor;
import com.systemspecs.remita.sdk.debit.CoreSdkAccount;
import com.systemspecs.remita.vending.extended.exception.UnknownException;
import com.systemspecs.remita.vending.extended.util.AccountUtil;
import com.systemspecs.remita.vending.vendingcommon.dto.response.TransactionResponse;
import com.systemspecs.remita.vending.vendingcommon.entity.FundRecoup;
import com.systemspecs.remita.vending.vendingcommon.entity.Transaction;
import com.systemspecs.remita.vending.vendingcommon.enums.SubscriptionType;
import com.systemspecs.remita.vending.vendingcommon.enums.SystemProduct;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import com.systemspecs.remita.vending.vendingcommon.factory.VendingServiceDelegateBean;
import com.systemspecs.remita.vending.vendingcommon.flight.factory.FlightProductVendingServiceDelegateBean;
import com.systemspecs.remita.vending.vendingcommon.flight.service.FlightProductAbstractVendingService;
import com.systemspecs.remita.vending.vendingcommon.repository.FundRecoupRepository;
import com.systemspecs.remita.vending.vendingcommon.service.AbstractVendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static com.systemspecs.remita.vending.extended.enums.ResponseStatus.INTERNAL_SERVER_ERROR;

@Service
@EnableAsync
@RequiredArgsConstructor
@Slf4j
@Component
public class AdminTransactionQueryService {

    private final FundRecoupRepository fundRecoupRepository;
    private final AccountUtil accountUtil;
    private final VendingServiceDelegateBean vendingServiceDelegateBean;
    private final FlightProductVendingServiceDelegateBean flightProductVendingServiceDelegateBean;
    private final CoreSdkAccount coreSdkAccount;


    public Transaction queryTransaction(Transaction transaction) {
        String subscriptionType = transaction.getSubscriptionType();
        String processorId = transaction.getProcessedWithFallback() != null && transaction.getProcessedWithFallback()
            ? transaction.getFallbackProcessorId()
            : transaction.getProcessorId();
        String systemProductType = transaction.getCategoryCode();

        // Step 1: Handle FLIGHT case separately
        if (SystemProduct.FLIGHT.name().equalsIgnoreCase(systemProductType)) {
            return handleFlightTransaction(transaction, subscriptionType, processorId);
        }

        // Step 2: Non-flight handling
        AbstractVendingService vendingService = getVendingService(processorId);
        TransactionResponse transactionResponse = vendingService.queryTransaction(transaction);
        log.info("Query result in core is => {}", transactionResponse);

        if (transactionResponse == null || transactionResponse.getStatus() == TransactionStatus.SYSTEM_ERROR) {
            return transaction;
        }

        // Async post-processing depending on result
        if (transactionResponse.getStatus() == TransactionStatus.TRANSACTION_FAILED) {
            asyncHandleFailedTransaction(transaction, transactionResponse, subscriptionType, vendingService,null);
        } else if (transactionResponse.getStatus() == TransactionStatus.SUCCESS) {
            asyncHandleSuccessTransaction(transaction, transactionResponse, subscriptionType, vendingService,null);
        }

        return vendingService.updateTransaction(transaction, transactionResponse);
    }

    /**
     * Extracted method to handle FLIGHT transactions
     */
    private Transaction handleFlightTransaction(Transaction transaction, String subscriptionType, String processorId) {
        FlightProductAbstractVendingService flightService = getFlightProductVendingService(processorId);
        TransactionResponse transactionResponse = flightService.queryTransaction(transaction);

        if (transactionResponse == null) {
            return transaction;
        }

        if (TransactionStatus.TRANSACTION_FAILED.getCode().equals(transactionResponse.getCode())) {
            asyncHandleFailedTransaction(transaction, transactionResponse, subscriptionType,null, flightService);
            return transaction;
        }

        if (transactionResponse.getStatus() == TransactionStatus.SUCCESS) {
            asyncHandleSuccessTransaction(transaction, transactionResponse, subscriptionType,null, flightService);
        }

        return flightService.updateTransaction(transaction, transactionResponse);
    }

    /**
     * Async handler for failed transactions
     */
    @Async
    public void asyncHandleFailedTransaction(Transaction transaction,
                                             TransactionResponse response,
                                             String subscriptionType,
                                             AbstractVendingService service,
                                             FlightProductAbstractVendingService flightService) {
        log.info("Async handling FAILED transaction for {}", transaction.getClientReference());

        if (subscriptionType.equalsIgnoreCase(String.valueOf(SubscriptionType.PREPAID))) {
            AccountDebitRequest accountReleaseRequest = accountUtil.getQueryAccountReaseRequest(transaction);
            CoreSDKResult releaseHeldFund = releaseHeldFund(accountReleaseRequest);
            log.info("release Held Fund is => {}", releaseHeldFund);
            if (!CoreSDKResult.RELEASED.equals(releaseHeldFund)) {
                FundRecoup fundRecoup = accountUtil.getHoldFundRecoup(transaction);
                fundRecoupRepository.save(fundRecoup);
            }
        }

        // Update the transaction state based on failed response
        service.updateTransaction(transaction, response);
    }

    /**
     * Async handler for successful transactions
     */
    @Async
    public void asyncHandleSuccessTransaction(Transaction transaction,
                                              TransactionResponse response,
                                              String subscriptionType,
                                              AbstractVendingService service,
                                              FlightProductAbstractVendingService flightService) {
        log.info("Async handling SUCCESS transaction for {}", transaction.getClientReference());

        if (subscriptionType.equalsIgnoreCase(String.valueOf(SubscriptionType.PREPAID))) {
            AccountDebitRequest accountDebitRequest = accountUtil.getQueryAccountDebitRequest(transaction);
            String vendProcessor = transaction.getProcessorId();

            CoreSDKResult releaseAndDebitResultMessage = releaseAndDebitFund(
                accountDebitRequest,
                VendProcessor.valueOf(vendProcessor.toUpperCase())
            );
            log.info("release And DebitResult Message is ....{}", releaseAndDebitResultMessage);

            if (releaseAndDebitResultMessage == CoreSDKResult.RELEASED) {
                FundRecoup fundRecoup = accountUtil.getReleasedFundRecoup(transaction);
                fundRecoupRepository.save(fundRecoup);
            } else if (releaseAndDebitResultMessage == CoreSDKResult.FAILED) {
                FundRecoup fundRecoup = accountUtil.getFailedFundRecoup(transaction);
                fundRecoupRepository.save(fundRecoup);
            }
        }

        // Update the transaction state based on success response
        service.updateTransaction(transaction, response);
    }

    private AbstractVendingService getVendingService(String processorId) {
        log.info(">>> Getting VendingService from processorId):{}", processorId);
        AbstractVendingService serviceBean = vendingServiceDelegateBean.getDelegate(processorId);
        if (Objects.isNull(serviceBean)) {
            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }
        return serviceBean;
    }

    private FlightProductAbstractVendingService getFlightProductVendingService(String processorId) {
        log.info(">>> Getting flightProductVendingService from processorId):{}", processorId);
        FlightProductAbstractVendingService serviceBean = flightProductVendingServiceDelegateBean.getDelegate(processorId);
        if (Objects.isNull(serviceBean)) {
            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }
        return serviceBean;
    }

    private CoreSDKResult releaseAndDebitFund(AccountDebitRequest accountDebitRequest, VendProcessor processor) {
        return coreSdkAccount.doVendingPrepaidReleaseDebit(accountDebitRequest, processor);
    }

    private CoreSDKResult releaseHeldFund(AccountDebitRequest accountDebitRequest) {
        return coreSdkAccount.releaseAmount(accountDebitRequest);
    }
}
