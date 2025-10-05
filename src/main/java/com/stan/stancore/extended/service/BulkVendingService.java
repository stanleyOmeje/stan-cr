package com.stan.stancore.extended.service;

import com.systemspecs.remita.dto.auth.MerchantDetailsDto;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.vendingcommon.dto.request.BulkVendingRequest;
import org.springframework.http.ResponseEntity;

public interface BulkVendingService {
    ResponseEntity<DefaultResponse> processBulkVending(BulkVendingRequest bulkVendingRequest, MerchantDetailsDto merchantDetailsDto);
}
