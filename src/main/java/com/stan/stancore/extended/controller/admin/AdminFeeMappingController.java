package com.stan.stancore.extended.controller.admin;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.vending.extended.dto.request.FeeMappingRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.service.FeeMappingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/admin/feemapping")
@RestController
@RequiredArgsConstructor
@Slf4j
public class AdminFeeMappingController {

    private final FeeMappingService feeMappingService;

    @ActivityTrail
    @PutMapping("/{code}")
    public ResponseEntity<DefaultResponse> updateFeeMapping(@RequestBody @Valid FeeMappingRequest request, @PathVariable String code) {
        log.info(">>> Inside update fee mapping Controller::transact with Request: {}", request);
        return feeMappingService.updateFeeMapping(request, code);
    }
}
