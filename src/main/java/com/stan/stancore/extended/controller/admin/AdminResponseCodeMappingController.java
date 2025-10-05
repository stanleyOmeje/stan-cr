package com.stan.stancore.extended.controller.admin;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.service.ResponseCodeMappingService;
import com.systemspecs.remita.vending.vendingcommon.dto.request.ResponseCodeMappingRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/admin/map")
@RestController
@RequiredArgsConstructor
public class AdminResponseCodeMappingController {

    private final ResponseCodeMappingService service;

    @ActivityTrail
    @PostMapping
    public ResponseEntity<DefaultResponse> addProcessorResponseCode(@RequestBody @Valid ResponseCodeMappingRequest request) {
        return service.addProcessorResponseMapping(request);
    }

    @ActivityTrail
    @GetMapping
    public ResponseEntity<DefaultResponse> getAllMappings(Pageable pageable) {
        return service.fetchAllResponseCode(pageable);
    }
}
