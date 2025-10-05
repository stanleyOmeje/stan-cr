package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import org.springframework.http.ResponseEntity;

public interface ProcessorService {
    ResponseEntity<DefaultResponse> getWalletBalance(String processorId);

    ResponseEntity<DefaultResponse> getAllProcessors();

    ResponseEntity<DefaultResponse> getProcessorByReference(String ref);

    ResponseEntity<DefaultResponse> getServices(String processorId);

}
