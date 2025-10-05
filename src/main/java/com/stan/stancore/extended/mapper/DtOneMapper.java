package com.stan.stancore.extended.mapper;

import com.systemspecs.remita.vending.dtonemodule.entity.DtOneNotification;
import com.systemspecs.remita.vending.vendingcommon.entity.Transaction;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Data
public class DtOneMapper {

    public DtOneNotification mapTransactionToNotification(Transaction transaction) {
        DtOneNotification notification = new DtOneNotification();
        notification.setProductCode(transaction.getProductCode());
        notification.setClientReference(transaction.getClientReference());
        notification.setStatus(transaction.getStatus());
        notification.setCreatedAt(transaction.getCreatedAt());
        notification.setConfirmationDate(transaction.getConfirmationDate());
        return notification;
    }
}
