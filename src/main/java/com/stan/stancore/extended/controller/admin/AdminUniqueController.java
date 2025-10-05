package com.stan.stancore.extended.controller.admin;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.vending.extended.service.UniqueService;
import com.systemspecs.remita.vending.vendingcommon.dto.request.AdminUniqueRequest;
import com.systemspecs.remita.vending.vendingcommon.dto.response.AdminResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/admin/unique")
@RestController
@RequiredArgsConstructor
public class AdminUniqueController {

    private final UniqueService uniqueService;

    @ActivityTrail
    @PostMapping("/customer-enquiry")
    public ResponseEntity<AdminResponse> customerEnquiry(@RequestBody @Valid AdminUniqueRequest request) {
        return uniqueService.performCustomerEnquiry(request);
    }

    @ActivityTrail
    @PostMapping("/shift-enquiry")
    public ResponseEntity<AdminResponse> shiftEnquiry(@RequestBody @Valid AdminUniqueRequest request) {
        return uniqueService.performShiftEnquiry(request);
    }

    @ActivityTrail
    @PostMapping("/vendor-info")
    public ResponseEntity<AdminResponse> vendorInformation(@RequestBody @Valid AdminUniqueRequest request) {
        return uniqueService.perfomVendorInformation(request);
    }

    @ActivityTrail
    @PostMapping("/criteria-type")
    public ResponseEntity<AdminResponse> vendorTransaction(@RequestBody @Valid AdminUniqueRequest request) {
        return uniqueService.performCriteriaType(request);
    }
}
