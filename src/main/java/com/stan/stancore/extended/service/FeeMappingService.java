package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.extended.dto.request.FeeMappingRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import org.springframework.http.ResponseEntity;

public interface FeeMappingService {
    ResponseEntity<DefaultResponse> updateFeeMapping(FeeMappingRequest feeMappingRequest, String productCode);
}
