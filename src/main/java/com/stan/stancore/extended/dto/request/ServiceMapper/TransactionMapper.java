package com.stan.stancore.extended.dto.request.ServiceMapper;

import com.systemspecs.remita.vending.extended.dto.request.DisplayTransaction;
import com.systemspecs.remita.vending.vendingcommon.entity.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TransactionMapper {

    public DisplayTransaction mapTransactionToDisplayTransaction(Transaction transaction) {
        log.info(">>> Mapping Transaction to DisplayTransaction");
        DisplayTransaction displayTransaction = new DisplayTransaction();
        displayTransaction.setAmount(transaction.getAmount());
        displayTransaction.setProductCode(transaction.getProductCode());
        displayTransaction.setCategoryCode(transaction.getCategoryCode());
        displayTransaction.setClientReference(transaction.getClientReference());
        displayTransaction.setStatus(transaction.getStatus().name());
        displayTransaction.setTransactionDate(transaction.getCreatedAt().toString());
        displayTransaction.setMerchantName(transaction.getMerchantName());
        displayTransaction.setSubscriptionType(transaction.getSubscriptionType());
        displayTransaction.setMerchantOrgId(transaction.getMerchantOrgId());
        return displayTransaction;
    }
}
