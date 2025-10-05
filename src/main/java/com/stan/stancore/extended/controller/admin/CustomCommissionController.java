package com.stan.stancore.extended.controller.admin;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.service.CustomCommissionService;
import com.systemspecs.remita.vending.vendingcommon.dto.request.CustomCommissionCreateRequest;
import com.systemspecs.remita.vending.vendingcommon.dto.request.CustomCommissionUpdateRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/admin/commissions")
@RestController
@RequiredArgsConstructor
public class CustomCommissionController {

    private final CustomCommissionService customCommissionService;

    @ActivityTrail
    @PostMapping
    public ResponseEntity<DefaultResponse> addCustomCommission(@RequestBody @Valid CustomCommissionCreateRequest commissionRequest) {
        return customCommissionService.addCustomCommission(commissionRequest);
    }

    @ActivityTrail
    @PutMapping("/{id}")
    public ResponseEntity<DefaultResponse> updateCustomCommission(
        @RequestBody @Valid CustomCommissionUpdateRequest commissionRequest,
        @PathVariable("id") Long id
    ) {
        return customCommissionService.updateCustomCommission(commissionRequest, id);
    }

    @GetMapping("")
    public ResponseEntity<DefaultResponse> fetchAllCommissions(
        @RequestParam(required = false, value = "merchantId") String merchantId,
        @RequestParam(required = false, value = "productCode") String productCode,
        @RequestParam(required = false, value = "processor") String processor,
        @RequestParam(required = false, defaultValue = "0", value = "page") int page,
        @RequestParam(required = false, defaultValue = "10", value = "pageSize") int pageSize
    ) {
        return customCommissionService.getAllCommissionsWithFilter(merchantId, productCode, processor, page, pageSize);
    }
}
