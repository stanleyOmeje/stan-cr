package com.stan.stancore.extended.controller.admin;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.vending.extended.service.ProcessorTransactionServiceByAdmin;
import com.systemspecs.remita.vending.vendingcommon.dto.request.AdminProcessorTransactionRequest;
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
@RequestMapping("/api/v1/admin/fetch-processor-transaction")
@RestController
@RequiredArgsConstructor
public class AdminProcessorTransactionController {

    private final ProcessorTransactionServiceByAdmin processorTransactionServiceByAdmin;

    @ActivityTrail
    @PostMapping
    public ResponseEntity<AdminResponse> fetchAllProcessorTransaction(@RequestBody @Valid AdminProcessorTransactionRequest request) {
        return processorTransactionServiceByAdmin.getAllProcessorTransaction(request);
    }
}
