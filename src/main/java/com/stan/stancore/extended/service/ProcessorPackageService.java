package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.vendingcommon.dto.request.ProcessorPackageRequest;
import org.springframework.http.ResponseEntity;

public interface ProcessorPackageService {
    ResponseEntity<DefaultResponse> createVendingPackage(ProcessorPackageRequest request,String processorId);

    ResponseEntity<DefaultResponse> fetchProcessPackage(String processorId);
}
