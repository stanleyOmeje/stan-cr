package com.stan.stancore.extended.controller;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.vending.dtonemodule.dto.request.OperatorsLookUpRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.service.OperatorsLookUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/vending/lookup/mobile-number")
@RequiredArgsConstructor
public class OperatorsLookUpController {

    private final OperatorsLookUpService service;

    @ActivityTrail
    @PostMapping
    public ResponseEntity<DefaultResponse> lookUpOperators(@RequestBody @Valid OperatorsLookUpRequest request) {
        return service.lookUpOperators(request);
    }
}
