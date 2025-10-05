package com.stan.stancore.extended.cron;

import com.systemspecs.remita.dto.account.AccountDebitRequest;
import com.systemspecs.remita.enumeration.CoreSDKResult;
import com.systemspecs.remita.enumeration.VendProcessor;
import com.systemspecs.remita.vending.extended.config.VendingCoreProperties;
import com.systemspecs.remita.vending.extended.util.AccountUtil;
import com.systemspecs.remita.vending.vendingcommon.entity.FundRecoup;
import com.systemspecs.remita.vending.vendingcommon.entity.Transaction;
import com.systemspecs.remita.vending.vendingcommon.enums.Action;
import com.systemspecs.remita.vending.vendingcommon.enums.DebitStatus;
import com.systemspecs.remita.vending.vendingcommon.repository.FundRecoupRepository;
import com.systemspecs.remita.vending.vendingcommon.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@AllArgsConstructor
public class FundRecoupCron {

    private final VendingCoreProperties vendingCoreProperties;
    private final FundRecoupRepository fundRecoupRepository;
    private final TransactionRepository transactionRepository;
    private final AccountUtil accountUtil;

    @Scheduled(cron = "${core.fund.recoup.cron: * * * * * *}")
    public void debitFundRecouped() {
        log.info("Inside debitReleasedTransactions of DebitUpdateCron");
        if (vendingCoreProperties.isEnableFundRecoupCron()) {
            List<FundRecoup> fundRecoups = fundRecoupRepository.findByStatus(DebitStatus.OPEN);
            if (fundRecoups.isEmpty()) {
                return;
            }
            fundRecoups.forEach(fundRecoup -> {
                try {
                    recoupFund(fundRecoup);
                } catch (Exception e) {
                    log.error("Ërror updating debitList with ref...{}...{}", fundRecoup.getReference(), e.getMessage());
                }
            });
        }
    }

    private void recoupFund(FundRecoup fundRecoup) {
        log.info("Inside recoupFund");
        try {
            String reference = fundRecoup.getReference();
            Optional<Transaction> optionalTransaction = transactionRepository.findByClientReference(reference);
            if (optionalTransaction.isEmpty()) {
                log.info("No transaction found for reference: {}", reference);
            } else {
                Transaction transaction = optionalTransaction.get();
                log.info("Corresponding transaction for debit reference:... {} is ...{}", reference, transaction);
                String subscriptionType = transaction.getSubscriptionType();
                if (!"prepaid".equalsIgnoreCase(subscriptionType)) {
                    log.info("Transaction subscription type is not prepaid");
                } else {
                    AccountDebitRequest accountDebitRequest = accountUtil.getQueryAccountDebitRequest(transaction);
                    log.info("accountDebitRequest in updateDebitList is...{}", accountDebitRequest);
                    CoreSDKResult debitResultMessage = CoreSDKResult.FAILED;
                    String vendProcessor = transaction.getProcessorId();
                    Action action = fundRecoup.getAction();
                    if (action != null) {
                        if (action.equals(Action.RELEASED_AND_DEBIT)) {
                            debitResultMessage =
                                accountUtil.releaseAndDebitFund(accountDebitRequest, VendProcessor.valueOf(vendProcessor.toUpperCase()));
                            if (CoreSDKResult.SUCCESS == debitResultMessage) {
                                log.info("Fund recoup debit successful ....");
                                fundRecoup.setStatus(DebitStatus.DEBITED);
                                fundRecoup.setDebitedDate(new Date());
                                fundRecoup.setMessage(String.valueOf(debitResultMessage));
                                fundRecoupRepository.save(fundRecoup);
                            } else if (debitResultMessage == CoreSDKResult.RELEASED) {
                                fundRecoup.setAction(Action.DEBIT);
                                fundRecoupRepository.save(fundRecoup);
                            } else {
                                fundRecoup.setMessage(String.valueOf(debitResultMessage));
                                fundRecoup.setStatus(DebitStatus.CLOSED);
                                fundRecoupRepository.save(fundRecoup);
                            }
                        } else if (action.equals(Action.DEBIT)) {
                            //Check if fund has been released
                            debitResultMessage =
                                accountUtil.debitFund(accountDebitRequest, VendProcessor.valueOf(vendProcessor.toUpperCase()));
                            if (CoreSDKResult.SUCCESS == debitResultMessage) {
                                log.info("Fund recoup debit successful ....");
                                fundRecoup.setStatus(DebitStatus.DEBITED);
                                fundRecoup.setDebitedDate(new Date());
                                fundRecoup.setMessage(String.valueOf(debitResultMessage));
                                fundRecoupRepository.save(fundRecoup);
                            } else {
                                fundRecoup.setMessage(String.valueOf(debitResultMessage));
                                fundRecoup.setStatus(DebitStatus.CLOSED);
                                fundRecoupRepository.save(fundRecoup);
                            }
                        } else if (action.equals(Action.RELEASE)) {
                            debitResultMessage = accountUtil.releaseHeldFund(accountDebitRequest);
                            if (CoreSDKResult.RELEASED == debitResultMessage) {
                                log.info("Fund recoup debit successful ....");
                                fundRecoup.setStatus(DebitStatus.RELEASED);
                                fundRecoup.setDebitedDate(new Date());
                                fundRecoup.setMessage(String.valueOf(debitResultMessage));
                                fundRecoupRepository.save(fundRecoup);
                            } else {
                                log.info("Fund recoup debit failed ....");
                                fundRecoup.setMessage(String.valueOf(debitResultMessage));
                                fundRecoup.setStatus(DebitStatus.CLOSED);
                                fundRecoupRepository.save(fundRecoup);
                            }
                        }
                        log.info("DebitResultMessage is ....{}", debitResultMessage);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
