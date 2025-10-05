package com.stan.stancore.extended.controller.admin;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.vending.extended.dto.request.CreateMerchantNotificationConfigRequest;
import com.systemspecs.remita.vending.extended.dto.request.UpdateMerchantNotificationConfigRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.service.MerchantNotificationConfigService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/admin/notification-config")
@RestController
@RequiredArgsConstructor
public class AdminMerchantNotificationConfigController {

    private final MerchantNotificationConfigService merchantNotificationConfigService;

    @ActivityTrail
    @PostMapping
    public ResponseEntity<DefaultResponse> createMerchantNotificationConfig(
        @RequestBody @Valid CreateMerchantNotificationConfigRequest createMerchantNotificationConfigRequest
    ) {
        return merchantNotificationConfigService.createMerchantNotificationConfig(createMerchantNotificationConfigRequest);
    }

    @ActivityTrail
    @PutMapping("/{merchantId}")
    public ResponseEntity<DefaultResponse> updateMerchantNotificationConfig(
        @RequestBody UpdateMerchantNotificationConfigRequest request,
        @PathVariable String merchantId
    ) {
        return merchantNotificationConfigService.updateMerchantNotificationConfig(request, merchantId);
    }
}
