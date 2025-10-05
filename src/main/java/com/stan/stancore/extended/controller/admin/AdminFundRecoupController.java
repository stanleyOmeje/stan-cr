package com.stan.stancore.extended.controller.admin;


import com.google.gson.Gson;
import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.vending.extended.dto.request.BulkDebitListRequest;
import com.systemspecs.remita.vending.extended.dto.request.FundRecoupRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.service.FundRecoupService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/admin/debit")
@RestController
@RequiredArgsConstructor
public class AdminFundRecoupController {
    Gson gson = new Gson();

    private final FundRecoupService fundRecoupService;

    @ActivityTrail
    @PostMapping("/fund-recoup")
    public ResponseEntity<DefaultResponse> createFundRecoup(@RequestBody @Valid FundRecoupRequest request) {
        return fundRecoupService.createFundRecoup(request);
    }



    @ActivityTrail
    @PostMapping(value = "bulk-fund-recoup")
    public ResponseEntity<DefaultResponse> createBulkFundRecoup(@RequestBody String payload) {
        BulkDebitListRequest bulkDebitListRequest = gson.fromJson(payload, BulkDebitListRequest.class);
        return fundRecoupService.createBulkFundRecoup(bulkDebitListRequest);
    }

}
