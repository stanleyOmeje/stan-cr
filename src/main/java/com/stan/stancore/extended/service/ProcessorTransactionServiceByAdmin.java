package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.vendingcommon.dto.request.AdminProcessorTransactionRequest;
import com.systemspecs.remita.vending.vendingcommon.dto.response.AdminResponse;
import org.springframework.http.ResponseEntity;

public interface ProcessorTransactionServiceByAdmin {
    ResponseEntity<AdminResponse> getAllProcessorTransaction(AdminProcessorTransactionRequest request);
    ResponseEntity<AdminResponse> getTransactionByReference(AdminProcessorTransactionRequest request);
}
