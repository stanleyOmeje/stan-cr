package com.stan.stancore.extended.cron;


import com.systemspecs.remita.vending.extended.config.VendingCoreProperties;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.service.impl.TransactionServiceImp;
import com.systemspecs.remita.vending.vendingcommon.dto.request.TransactionDataRequest;
import com.systemspecs.remita.vending.vendingcommon.dto.request.TransactionRequest;
import com.systemspecs.remita.vending.vendingcommon.dto.response.LocalTransactionResponse;
import com.systemspecs.remita.vending.vendingcommon.entity.RevendItems;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import com.systemspecs.remita.vending.vendingcommon.repository.RevendItemsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BulkRevendSchedule {

    private final RevendItemsRepository revendItemsRepository;
    private final VendingCoreProperties properties;
    private final TransactionServiceImp transactionServiceImp;

    @Scheduled(cron = "${revend.cron.duration: * * * * * *}")
    public void processBulkRevend() {
        int pageNumber = 0;
        while (properties.isEnabledBulkRevendCron()) {
            log.info("Inside processBulkRevend");
            PageRequest page = PageRequest.of(pageNumber, properties.getPageSize());
            List<RevendItems> revendItemsList = revendItemsRepository.findAllByVendStatus(TransactionStatus.PENDING, page);

            if (revendItemsList.isEmpty()) {
                break;
            }
            revendItemsList.forEach(item -> {
                TransactionRequest transactionRequest = mapRevendItemToTransactionRequest(item);
                //   ResponseEntity<DefaultResponse> response = transactionServiceImp.reVend(transactionRequest);
                ResponseEntity<DefaultResponse> response = null;
                log.info("Response from bulk revend: {}", response);
                if (response != null && response.getBody() != null) {
                    if (TransactionStatus.SUCCESS.getCode().equals(response.getBody().getStatus())) {
                        LocalTransactionResponse finalVendResponse = (LocalTransactionResponse) response
                            .getBody()
                            .getData();
                        item.setVendStatus(TransactionStatus.SUCCESS);
                        item.setMessage(response.getBody().getMessage());
                    } else if (TransactionStatus.PENDING.getCode().equals(response.getBody().getStatus())) {
                        item.setVendStatus(TransactionStatus.PENDING);
                        item.setMessage(response.getBody().getMessage());
                    } else if (TransactionStatus.TRANSACTION_FAILED.getCode().equals(response.getBody().getStatus())) {
                        item.setVendStatus(TransactionStatus.TRANSACTION_FAILED);
                        item.setMessage(response.getBody().getMessage());
                    } else {
                        item.setVendStatus(TransactionStatus.TRANSACTION_FAILED);
                        item.setMessage(response.getBody().getMessage());
                    }
                } else {
                    item.setVendStatus(TransactionStatus.UNKNOWN_CODE);
                    item.setMessage(TransactionStatus.UNKNOWN_CODE.getMessage());
                }

                revendItemsRepository.save(item);
            });
            pageNumber++;
        }
    }

    private TransactionRequest mapRevendItemToTransactionRequest(RevendItems item) {
        TransactionRequest request = new TransactionRequest();
        request.setProductCode(item.getProductCode());
        request.setClientReference(item.getPaymentIdentifier());
        TransactionDataRequest dataRequest = new TransactionDataRequest();
        dataRequest.setAccountNumber(item.getAccountNumber());
        dataRequest.setPhoneNumber(item.getPhoneNumber());
        request.setData(dataRequest);

        return request;
    }
}


