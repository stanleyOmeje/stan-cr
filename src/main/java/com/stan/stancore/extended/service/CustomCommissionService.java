package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.vendingcommon.dto.request.CustomCommissionCreateRequest;
import com.systemspecs.remita.vending.vendingcommon.dto.request.CustomCommissionUpdateRequest;
import org.springframework.http.ResponseEntity;

public interface CustomCommissionService {
    ResponseEntity<DefaultResponse> addCustomCommission(CustomCommissionCreateRequest request);
    ResponseEntity<DefaultResponse> updateCustomCommission(CustomCommissionUpdateRequest commissionUpdateRequest, Long id);
    ResponseEntity<DefaultResponse> getAllCommissionsWithFilter(
        String merchantId,
        String productCode,
        String processor,
        int page,
        int pageSize
    );
}
