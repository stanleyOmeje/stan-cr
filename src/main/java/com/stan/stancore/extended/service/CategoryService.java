package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.extended.dto.request.CreateCategoryRequest;
import com.systemspecs.remita.vending.extended.dto.request.UpdateCategoryRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface CategoryService {
    ResponseEntity<DefaultResponse> createCategory(CreateCategoryRequest category);
    ResponseEntity<DefaultResponse> updateCategory(UpdateCategoryRequest reuquest, String code);
    ResponseEntity<DefaultResponse> fetchAllCategory(Pageable pageable);
    ResponseEntity<DefaultResponse> fetchCategoryByCode(String code);
}
