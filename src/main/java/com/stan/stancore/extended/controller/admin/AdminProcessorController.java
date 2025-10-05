package com.stan.stancore.extended.controller.admin;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.service.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/admin/processor")
@RestController
@RequiredArgsConstructor
public class AdminProcessorController {

    private final ProcessorService processorService;

    @ActivityTrail
    @GetMapping("/{processorId}/balance")
    public ResponseEntity<DefaultResponse> getBalance(@PathVariable String processorId) {
        return processorService.getWalletBalance(processorId);
    }

    @ActivityTrail
    @GetMapping
    public ResponseEntity<DefaultResponse> getAllProcessors() {
        return processorService.getAllProcessors();
    }

    @ActivityTrail
    @GetMapping("/{reference}")
    public ResponseEntity<DefaultResponse> getProcessorByReference(@PathVariable String reference) {
        return processorService.getProcessorByReference(reference);
    }

    @ActivityTrail
    @GetMapping("/{processorId}/services")
    public ResponseEntity<DefaultResponse> getServices(@PathVariable String processorId) {
        return processorService.getServices(processorId);
    }
}
