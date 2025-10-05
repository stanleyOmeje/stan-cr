package com.stan.stancore.extended.cron;

import com.systemspecs.remita.dto.account.AccountDebitRequest;
import com.systemspecs.remita.enumeration.CoreSDKResult;
import com.systemspecs.remita.enumeration.VendProcessor;
import com.systemspecs.remita.vending.extended.config.VendingCoreProperties;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.util.AccountUtil;
import com.systemspecs.remita.vending.extended.util.PublishUtil;
import com.systemspecs.remita.vending.vendingcommon.dto.response.TransactionResponse;
import com.systemspecs.remita.vending.vendingcommon.entity.ExtraToken;
import com.systemspecs.remita.vending.vendingcommon.entity.FundRecoup;
import com.systemspecs.remita.vending.vendingcommon.entity.Transaction;
import com.systemspecs.remita.vending.vendingcommon.enums.SubscriptionType;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import com.systemspecs.remita.vending.vendingcommon.factory.VendingServiceDelegateBean;
import com.systemspecs.remita.vending.vendingcommon.repository.FundRecoupRepository;
import com.systemspecs.remita.vending.vendingcommon.repository.TransactionRepository;
import com.systemspecs.remita.vending.vendingcommon.service.AbstractVendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionUpdateCron {
    private final VendingCoreProperties vendingCoreProperties;
    private final TransactionRepository transactionRepository;
    private final VendingServiceDelegateBean vendingServiceDelegateBean;
    private final PublishUtil publishUtil;
    private final AccountUtil accountUtil;
    private final FundRecoupRepository fundRecoupRepository;


    @Scheduled(cron = "${core.transaction.update.cron:0 0/5 * * * ?}")
    public void updatePendingTransactions() {
        if (!vendingCoreProperties.isEnableTransactionUpdateCron()) {
            return;
        }
        LocalDate targetDate = LocalDate.now().minusDays(1);
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime now = LocalDateTime.now(); // current timestamp when cron runs
        Date startDate = Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());
        List<Transaction> pendingTransactions = transactionRepository
            .getPendingTransactionsForDate(startDate, endDate, List.of(TransactionStatus.PENDING));
        log.info("CronReconciling {} transactions size for startDate {} and endDate {} ", pendingTransactions.size(), startDate, endDate);
        if (pendingTransactions.isEmpty()) {
            log.info("No pending transactions found for reconciliation on {}", targetDate);
            return;
        }
        pendingTransactions.forEach(transaction -> {
            try {
                updatePendingTransaction(transaction);
            } catch (Exception e) {
                log.error("Error updating transaction with ref {}: {}",
                    transaction.getClientReference(), e.getMessage(), e);
            }
        });
    }


    private void updatePendingTransaction(Transaction transaction) {
        String processorId = resolveProcessorId(transaction);
        if (StringUtils.isBlank(processorId)) {
            log.warn("Skipping transaction {} due to invalid processor ID", transaction.getClientReference());
            return;
        }

        AbstractVendingService vendingService = getVendingService(processorId);
        if (vendingService == null) {
            log.warn("No vending service found for processorId {}", processorId);
            return;
        }

        TransactionResponse response = vendingService.queryTransaction(transaction);
        log.info("query transaction response {} with internalReference {}", response, transaction.getInternalReference());
        if (response == null) {
            log.warn("Null response for transaction {}", transaction.getClientReference());
            return;
        }

        updateStatus(response, transaction, vendingService);

    }

    private void handleFailedTransaction(Transaction transaction, AbstractVendingService vendingService, TransactionResponse response) {
        String subscriptionType = transaction.getSubscriptionType();

        if (SubscriptionType.PREPAID.name().equalsIgnoreCase(subscriptionType)) {
            AccountDebitRequest accountReleaseRequest = accountUtil.getQueryAccountReaseRequest(transaction);
            CoreSDKResult releaseHeldFund = accountUtil.releaseHeldFund(accountReleaseRequest);

            log.info("Release held fund for transaction {} => {}", transaction.getClientReference(), releaseHeldFund);

            if (CoreSDKResult.RELEASED.equals(releaseHeldFund)) {
                vendingService.updateTransaction(transaction, response);
            }
        } else {
            FundRecoup fundRecoup = accountUtil.getHoldFundRecoup(transaction);
            fundRecoupRepository.save(fundRecoup);
            vendingService.updateTransaction(transaction, response);
        }
    }

    private void handleSuccessfulTransaction(Transaction transaction, AbstractVendingService vendingService, TransactionResponse response) {
        log.info("Transaction {} successful on query", transaction.getClientReference());

        if (SubscriptionType.PREPAID.name().equalsIgnoreCase(transaction.getSubscriptionType())) {
            AccountDebitRequest accountDebitRequest = accountUtil.getQueryAccountDebitRequest(transaction);
            CoreSDKResult debitResult = accountUtil.releaseAndDebitFund(
                accountDebitRequest,
                VendProcessor.valueOf(transaction.getProcessorId().toUpperCase())
            );

            log.info("Debit result for transaction {} => {}", transaction.getClientReference(), debitResult);

            FundRecoup fundRecoup = (debitResult == CoreSDKResult.RELEASED)
                ? accountUtil.getReleasedFundRecoup(transaction)
                : accountUtil.getFailedFundRecoup(transaction);

            fundRecoupRepository.save(fundRecoup);
        }

        Transaction updatedTransaction = vendingService.updateTransaction(transaction, response);
        if (updatedTransaction != null) {
            handleTokenPublishing(updatedTransaction);
        }
    }

    private void handleTokenPublishing(Transaction updatedTransaction) {
        String token = updatedTransaction.getToken();

        if (StringUtils.isBlank(token) && updatedTransaction.getExtraToken() != null) {
            token = updatedTransaction.getExtraToken().getStandardTokenValue();
        }

        if (StringUtils.isNotBlank(token)) {
            ExtraToken extraToken = updatedTransaction.getExtraToken();
            if (extraToken == null) {
                extraToken = new ExtraToken();
                updatedTransaction.setExtraToken(extraToken);
            }
            extraToken.setStandardTokenValue(token);

            publishUtil.publishVendNotification(extraToken, updatedTransaction.getClientReference());
            log.info("Published vend notification for transaction {}", updatedTransaction.getClientReference());
        }
    }

    private AbstractVendingService getVendingService(String processorId) {
        return Objects.isNull(processorId) ? null : vendingServiceDelegateBean.getDelegate(processorId);
    }

    private String resolveProcessorId(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        Boolean processedWithFallback = transaction.getProcessedWithFallback();

        // If fallback was used
        if (Boolean.TRUE.equals(processedWithFallback)) {
            String fallbackProcessor = transaction.getFallbackProcessorId();
            if (StringUtils.isBlank(fallbackProcessor)) {
                log.error("Transaction {} marked as processed with fallback but fallbackProcessorId is null/blank!",
                    transaction.getClientReference());
                return null; // fail safe → skip this transaction
            }
            return fallbackProcessor;
        }

        // Otherwise return the primary processor
        return transaction.getProcessorId();
    }

    /**
     * Manual reconciliation for a single transaction.
     * Looks up the transaction by clientReference, queries 3rd party,
     * updates our DB, and returns a DefaultResponse.
     */
    public DefaultResponse reconcileTransaction(String internalReference) {
        Transaction transaction = transactionRepository.findByInternalReference(internalReference)
            .orElse(null);

        if (transaction == null) {
            log.warn("No transaction found with internalReference {}", internalReference);
            return new DefaultResponse("Transaction not found", "FAILED");
        }

        String processorId = resolveProcessorId(transaction);
        if (StringUtils.isBlank(processorId)) {
            log.warn("Skipping transaction {} due to invalid processor ID..", transaction.getClientReference());
            return new DefaultResponse("Invalid processor ID", "FAILED");
        }

        AbstractVendingService vendingService = getVendingService(processorId);
        if (vendingService == null) {
            log.warn("No vending service found for processorId.. {}", processorId);
            return new DefaultResponse("No vending service found for processor", "FAILED");
        }

        TransactionResponse response = vendingService.queryTransaction(transaction);
        if (response == null) {
            log.warn("Null response for transaction.. {}", transaction.getClientReference());
            return new DefaultResponse("Null response from processor", "FAILED");
        }

        updateStatus(response, transaction, vendingService);

        // 5. Wrap in DefaultResponse
        return new DefaultResponse(
            "Transaction reconciled successfully",
            "SUCCESS",
            response
        );
    }

    private void updateStatus(TransactionResponse response, Transaction transaction, AbstractVendingService vendingService) {
        switch (response.getStatus()) {
            case TRANSACTION_FAILED:
                handleFailedTransaction(transaction, vendingService, response);
                break;

            case SUCCESS:
                handleSuccessfulTransaction(transaction, vendingService, response);
                break;

            default:
                log.info("Transaction {} remains in status {}",
                    transaction.getClientReference(), response.getStatus());
                break;
        }
    }


}
