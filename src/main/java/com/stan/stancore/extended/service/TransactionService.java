package com.stan.stancore.extended.service;

import com.systemspecs.remita.dto.auth.MerchantDetailsDto;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.vendingcommon.dto.request.*;
import org.springframework.http.ResponseEntity;

public interface TransactionService {
    ResponseEntity<DefaultResponse> fetchAllTransaction(TransactionPage page, TransactionSearchCriteria searchCriteria);
    ResponseEntity<DefaultResponse> performTransaction(TransactionRequest transactionRequest, MerchantDetailsDto merchantDetailsDto);
    ResponseEntity<DefaultResponse> performTransactionV2(TransactionRequest transactionRequest, MerchantDetailsDto merchantDetailsDto);
    ResponseEntity<DefaultResponse> getTransactionByReference(String internalReference, String merchantOrgId);

    ResponseEntity<DefaultResponse> fetchAllTransactionsByAdmin(TransactionSearchCriteria criteria, TransactionPage page);

    ResponseEntity<DefaultResponse> getTransactionByInternalReferenceByAdmin(String reference);

    ResponseEntity<DefaultResponse> getBulkTransactionByClientReference(
        BulkTransactionPage bulkTransactionPage,
        BulkTransactionSearchCriteria bulkTransactionSearchCriteria
    );
    ResponseEntity<DefaultResponse> performRevendTransaction(TransactionRevendRequest transactionRequest);

    ResponseEntity<DefaultResponse> performRevendTransactionV2(TransactionRequest transactionRequest, MerchantDetailsDto merchantDetailsDto);

    ResponseEntity<DefaultResponse> getTransactionByPaymentIdentifier(String paymentIdentifier, String merchantOrgId);
    ResponseEntity<DefaultResponse> adminRequeryTransaction(String internalReference);
}
