package com.stan.stancore.extended.controller.admin;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.service.ProcessorPackageService;
import com.systemspecs.remita.vending.vendingcommon.dto.request.ProcessorPackageRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/admin/processor-package")
@RestController
@RequiredArgsConstructor
public class AdminProcessorPackageController {

    private final ProcessorPackageService processorPackageService;

    @ActivityTrail
    @PostMapping("/{processorId}")
    public ResponseEntity<DefaultResponse> createOrUpdateVendingProcessorPackages(
        @RequestBody @Valid ProcessorPackageRequest request,
        @PathVariable String processorId
    ) {
        return processorPackageService.createVendingPackage(request, processorId);
    }

    @ActivityTrail
    @GetMapping("/{processorId}")
    public ResponseEntity<DefaultResponse> getProcessorPackages(@PathVariable String processorId) {
        return processorPackageService.fetchProcessPackage(processorId);
    }
}
