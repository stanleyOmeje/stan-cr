package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.extended.dto.request.VendingRouteConfigRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface VendingRouteConfigService {
    ResponseEntity<DefaultResponse> createVendingRoute(VendingRouteConfigRequest vendingRouteConfigRequest);
    ResponseEntity<DefaultResponse> updateVendingRoute(VendingRouteConfigRequest vendingRouteConfigRequest, String productCode);
    ResponseEntity<DefaultResponse> deleteVendingRouteByProductCode(String productCode);

    ResponseEntity<DefaultResponse> fetchAllVendingRoute(Pageable pageable);

    ResponseEntity<DefaultResponse> getRouteConfigByProductCode(String productCode);
}
