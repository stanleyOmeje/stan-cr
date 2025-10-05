package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.extended.dto.request.BulkDebitListRequest;
import com.systemspecs.remita.vending.extended.dto.request.FundRecoupRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import org.springframework.http.ResponseEntity;

public interface FundRecoupService {
    ResponseEntity<DefaultResponse> createFundRecoup(FundRecoupRequest request);

    ResponseEntity<DefaultResponse> createBulkFundRecoup(BulkDebitListRequest bulkDebitListRequest);
}
