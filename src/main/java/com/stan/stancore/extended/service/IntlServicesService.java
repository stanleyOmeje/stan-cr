package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import org.springframework.http.ResponseEntity;

public interface IntlServicesService {
    ResponseEntity<DefaultResponse> fetchServices();
    ResponseEntity<DefaultResponse> fetchServiceById(Long id);
}
