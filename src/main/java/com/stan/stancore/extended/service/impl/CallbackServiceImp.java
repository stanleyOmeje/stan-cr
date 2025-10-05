package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.dtonemodule.dto.response.IntlQueryResponse.CallbackResponse;
import com.systemspecs.remita.vending.dtonemodule.dto.response.IntlQueryResponse.QueryRes;
import com.systemspecs.remita.vending.dtonemodule.entity.DtOneNotification;
import com.systemspecs.remita.vending.dtonemodule.repository.DtOneNotificationRepository;
import com.systemspecs.remita.vending.dtonemodule.service.DtOneVendingService;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.mapper.DtOneMapper;
import com.systemspecs.remita.vending.extended.service.CallbackService;
import com.systemspecs.remita.vending.vendingcommon.entity.Transaction;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import com.systemspecs.remita.vending.vendingcommon.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Data
@Slf4j
@Service
@AllArgsConstructor
public class CallbackServiceImp implements CallbackService {

    private final TransactionRepository transactionRepository;
    private final DtOneVendingService dtOneVendingService;
    private final DtOneNotificationRepository repository;

    private final DtOneMapper dtOneMapper;

    @Override
    public ResponseEntity<DefaultResponse> processCallback(QueryRes queryRes) {
        log.info("Processing call back with request..{}", queryRes);
        String ref = queryRes.getExternal_id();
        Transaction transaction = new Transaction();
        Optional<Transaction> optionalTransaction = transactionRepository.findByClientReference(ref);
        if (optionalTransaction.isPresent()) {
            if (optionalTransaction.get().getStatus().name().equals(TransactionStatus.CONFIRMED.name())) {
                transaction = updateIntlTransaction(optionalTransaction.get(), queryRes);
            }
        } else {
            throw new NotFoundException("Transaction with ref " + ref + " does not exit");
        }
        log.info("Inside Callback method with transactionRes ...{}", queryRes);
        CallbackResponse response = new CallbackResponse();
        response.setProductCode(transaction.getProductCode());
        response.setStatus(transaction.getStatus());
        response.setExternalId(transaction.getInternalReference());
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setData(response);

        DtOneNotification notification = dtOneMapper.mapTransactionToNotification(transaction);
        repository.save(notification);

        return ResponseEntity.ok(defaultResponse);
    }

    private Transaction updateIntlTransaction(Transaction transaction, QueryRes response) {
        transaction.setUpdatedAt(new Date());
        if (response.getStatus().getMessage().equals(TransactionStatus.COMPLETED.name())) {
            transaction.setStatus(TransactionStatus.COMPLETED);
            transaction.setResponseMessage(TransactionStatus.COMPLETED.getMessage());
        }
        transaction = transactionRepository.save(transaction);
        return transaction;
    }
}
