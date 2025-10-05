package com.stan.stancore.extended.cron;

import com.systemspecs.remita.dto.account.AccountDebitRequest;
import com.systemspecs.remita.enumeration.AccountType;
import com.systemspecs.remita.enumeration.CoreSDKResult;
import com.systemspecs.remita.enumeration.VendProcessor;
import com.systemspecs.remita.sdk.debit.CoreSdkAccount;
import com.systemspecs.remita.vending.extended.config.VendingCoreProperties;
import com.systemspecs.remita.vending.extended.util.ReferenceUtil;
import com.systemspecs.remita.vending.vendingcommon.entity.PostPaymentHistory;
import com.systemspecs.remita.vending.vendingcommon.entity.PostpaidSettlement;
import com.systemspecs.remita.vending.vendingcommon.enums.PostpaidSettlementStatus;
import com.systemspecs.remita.vending.vendingcommon.repository.PostPaymentHistoryRepository;
import com.systemspecs.remita.vending.vendingcommon.repository.PostpaidSettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostpaidTransactionCron {

    private final PostpaidSettlementRepository settlementRepository;
    private final CoreSdkAccount coreSdkAccount;
    private final PostPaymentHistoryRepository postPaymentHistoryRepository;
    private final VendingCoreProperties properties;

    @Scheduled(cron = "${postpaid.processing.cron}")
    @Transactional
    public void processUnsettledTransactions() {
        if (properties.isEnablePostpaidCron()) {
            List<PostpaidSettlement> unsettledTransactions = settlementRepository.findByStatus(PostpaidSettlementStatus.UNSETTLED);
            String postPaymentRef = ReferenceUtil.generateInternalReference();

            Map<String, BigDecimal> groupedTransactions = unsettledTransactions
                .stream()
                .collect(
                    Collectors.groupingBy(
                        PostpaidSettlement::getMerchantAccountNumber,
                        Collectors.reducing(BigDecimal.ZERO, PostpaidSettlement::getAmount, BigDecimal::add)
                    )
                );

            for (Map.Entry<String, BigDecimal> entry : groupedTransactions.entrySet()) {
                String accountNumber = entry.getKey();
                BigDecimal totalAmount = entry.getValue();
                PostpaidSettlement sampleSettlement = unsettledTransactions
                    .stream()
                    .filter(s -> s.getMerchantAccountNumber().equals(accountNumber))
                    .findFirst()
                    .get();

                AccountDebitRequest accountDebitRequest = getAccountDebitRequest(sampleSettlement, totalAmount);
                VendProcessor processor = VendProcessor.valueOf(sampleSettlement.getProcessorId().toUpperCase());
                CoreSDKResult debitResult = coreSdkAccount.doPostVendingDebit(accountDebitRequest, processor);

                if (debitResult.equals(CoreSDKResult.SUCCESS)) {
                    unsettledTransactions
                        .stream()
                        .filter(s -> s.getMerchantAccountNumber().equals(accountNumber))
                        .forEach(s -> {
                            s.setStatus(PostpaidSettlementStatus.SETTLED);
                            settlementRepository.save(s);
                        });

                    PostPaymentHistory paymentHistory = new PostPaymentHistory();
                    paymentHistory.setPostPaymentRef(postPaymentRef);
                    paymentHistory.setMerchantAccountNumber(accountNumber);
                    paymentHistory.setMerchantBankCode(sampleSettlement.getMerchantBankCode());
                    paymentHistory.setPaymentDate(LocalDateTime.now());
                    paymentHistory.setTotalAmount(totalAmount);
                    postPaymentHistoryRepository.save(paymentHistory);

                    log.info("Processed and saved payment history for account: {}", accountNumber);
                } else {
                    log.error("Failed to process debit for account: {}", accountNumber);
                }
            }
        }
    }

    private static AccountDebitRequest getAccountDebitRequest(PostpaidSettlement postpaidSettlement, BigDecimal totalAmount) {
        AccountDebitRequest accountDebitRequest = new AccountDebitRequest();
        accountDebitRequest.setAccountNumber(postpaidSettlement.getMerchantAccountNumber());
        accountDebitRequest.setBankCode(postpaidSettlement.getMerchantBankCode());
        accountDebitRequest.setAccountType(AccountType.POSTPAID);
        accountDebitRequest.setAmount(totalAmount);
        accountDebitRequest.setDebitRef(postpaidSettlement.getClientReference());
        accountDebitRequest.setServiceName("remita-wallet");
        accountDebitRequest.setOrgId(postpaidSettlement.getMerchantOrgId());
        return accountDebitRequest;
    }
}
