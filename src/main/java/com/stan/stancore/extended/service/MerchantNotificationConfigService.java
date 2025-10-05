package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.extended.dto.request.CreateMerchantNotificationConfigRequest;
import com.systemspecs.remita.vending.extended.dto.request.UpdateMerchantNotificationConfigRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import org.springframework.http.ResponseEntity;

public interface MerchantNotificationConfigService {
    ResponseEntity<DefaultResponse> createMerchantNotificationConfig(CreateMerchantNotificationConfigRequest request);
    ResponseEntity<DefaultResponse> updateMerchantNotificationConfig(UpdateMerchantNotificationConfigRequest request, String merchantId);
}
