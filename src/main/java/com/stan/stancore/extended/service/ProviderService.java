package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.extended.dto.request.CreateProviderRequest;
import com.systemspecs.remita.vending.extended.dto.request.UpdateProviderRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface ProviderService {
    ResponseEntity<DefaultResponse> createProvider(CreateProviderRequest request);
    ResponseEntity<DefaultResponse> updateProvider(UpdateProviderRequest request, String code);

    ResponseEntity<DefaultResponse> fetchAllProviderWithFilterByAdmin(String code, String name, String description, int page, int pageSize);
    ResponseEntity<DefaultResponse> fetchAllProviderWithFilter(String code, String name, String description, int page, int pageSize);

    ResponseEntity<DefaultResponse> fetchProviderByCategory(String category, Pageable pageable);
}
