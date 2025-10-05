package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import org.springframework.http.ResponseEntity;

public interface MobileNumberLookUpService {
    ResponseEntity<DefaultResponse> lookUpMobileNumber(String number);
}
