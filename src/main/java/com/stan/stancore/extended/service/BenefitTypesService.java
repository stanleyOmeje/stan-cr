package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import org.springframework.http.ResponseEntity;

public interface BenefitTypesService {
    ResponseEntity<DefaultResponse> fetchBenefitTypes();
}
