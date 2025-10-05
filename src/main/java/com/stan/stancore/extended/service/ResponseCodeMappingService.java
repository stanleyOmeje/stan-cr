package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.vendingcommon.dto.request.ResponseCodeMappingRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface ResponseCodeMappingService {
    ResponseEntity<DefaultResponse> addProcessorResponseMapping(ResponseCodeMappingRequest request);
    ResponseEntity<DefaultResponse> fetchAllResponseCode(Pageable pageable);
}
