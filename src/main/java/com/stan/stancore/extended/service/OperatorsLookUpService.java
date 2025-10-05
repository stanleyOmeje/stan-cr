package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.dtonemodule.dto.request.OperatorsLookUpRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import org.springframework.http.ResponseEntity;

public interface OperatorsLookUpService {
    ResponseEntity<DefaultResponse> lookUpOperators(OperatorsLookUpRequest request);
}
