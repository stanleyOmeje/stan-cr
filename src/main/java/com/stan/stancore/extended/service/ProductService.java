package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.extended.dto.request.CreateProductRequest;
import com.systemspecs.remita.vending.extended.dto.request.UpdateProductRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.vendingcommon.enums.ProductType;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ProductService {
    ResponseEntity<DefaultResponse> createProduct(CreateProductRequest createProductRequest);
    ResponseEntity<DefaultResponse> updateProduct(UpdateProductRequest updateProductRequest, String code);
    ResponseEntity<DefaultResponse> fetchAllProductWithFilterByAdmin(
        String categoryCode,
        String currencyCode,
        String country,
        String countryCode,
        ProductType productType,
        String provider,
        String code,
        int page,
        int pageSize
    );
    ResponseEntity<DefaultResponse> fetchAllProductWithFilter(
        String categoryCode,
        String currencyCode,
        String country,
        String countryCode,
        ProductType productType,
        String provider,
        String code,
        int page,
        int pageSize
    );
    ResponseEntity<DefaultResponse> fetchProductByCode(String code);
    ResponseEntity<DefaultResponse> fetchProductByCategory(String categoryName, Pageable pageable);

    ResponseEntity<DefaultResponse> processProductPrice(String productId);

    DefaultResponse bulkCreateProduct(List<CreateProductRequest> request);
}
