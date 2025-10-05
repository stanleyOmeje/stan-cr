package com.stan.stancore.extended.controller.admin;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.vending.extended.dto.request.VendingRouteConfigRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.service.VendingRouteConfigService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/admin/route")
@RestController
@RequiredArgsConstructor
public class AdminConfigRouteController {

    private final VendingRouteConfigService vendingRouteConfigService;

    @ActivityTrail
    @PostMapping
    public ResponseEntity<DefaultResponse> createVendingServiceRouteConfig(@RequestBody @Valid VendingRouteConfigRequest request) {
        return vendingRouteConfigService.createVendingRoute(request);
    }

    @ActivityTrail
    @PutMapping("/{code}")
    public ResponseEntity<DefaultResponse> updateRouteConfig(
        @RequestBody @Valid VendingRouteConfigRequest request,
        @PathVariable("code") String code
    ) {
        return vendingRouteConfigService.updateVendingRoute(request, code);
    }

    @ActivityTrail
    @GetMapping
    public ResponseEntity<DefaultResponse> getAllRouteConfig(Pageable pageable) {
        return vendingRouteConfigService.fetchAllVendingRoute(pageable);
    }

    @ActivityTrail
    @DeleteMapping("/{code}")
    public ResponseEntity<DefaultResponse> deleteRouteConfigByProductCode(@PathVariable("code") String code) {
        return vendingRouteConfigService.deleteVendingRouteByProductCode(code);
    }

    @ActivityTrail
    @GetMapping("/{productCode}")
    public ResponseEntity<DefaultResponse> getRouteConfigByProductCode(@PathVariable("productCode") String productCode) {
        return vendingRouteConfigService.getRouteConfigByProductCode(productCode);
    }
}
