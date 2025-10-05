package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.extended.dto.request.CreateMerchantNotificationConfigRequest;
import com.systemspecs.remita.vending.extended.dto.request.UpdateMerchantNotificationConfigRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.AlreadyExistException;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.MerchantNotificationConfigService;
import com.systemspecs.remita.vending.vendingcommon.entity.MerchantNotificationConfig;
import com.systemspecs.remita.vending.vendingcommon.repository.MerchantNotificationConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Service
public class MerchantNotificationConfigServiceImpl implements MerchantNotificationConfigService {

    private final MerchantNotificationConfigRepository merchantNotificationConfigRepository;
    private static final String MERCHANT_NOTIFICATION_CONFIG = "Merchant Notification Config";

    @Override
    public ResponseEntity<DefaultResponse> createMerchantNotificationConfig(CreateMerchantNotificationConfigRequest request) {
        log.info(">>> Creating MerchantNotificationConfig with request: {}", request);
        String merchanId = request.getMerchantId().replaceAll("\\s+", "-");
        Optional<MerchantNotificationConfig> smsConfigCheck = merchantNotificationConfigRepository.findFirstByMerchantId(merchanId);
        if (smsConfigCheck.isPresent()) {
            log.info("MerchantNotificationConfig already exists: ");
            throw new AlreadyExistException(MERCHANT_NOTIFICATION_CONFIG + ResponseStatus.ALREADY_EXIST.getMessage());
        }

        MerchantNotificationConfig merchantNotificationConfig = new MerchantNotificationConfig();
        merchantNotificationConfig.setMerchantId(merchanId);
        merchantNotificationConfig.setEnableSms(request.getEnableSms());
        merchantNotificationConfig.setEnableEmail(request.getEnableEmail());
        merchantNotificationConfig.setNotificatonUrl(request.getNotificatonUrl());
        merchantNotificationConfig.setCreatedAt(new Date());

        merchantNotificationConfigRepository.save(merchantNotificationConfig);
        DefaultResponse defaultResponse = new DefaultResponse(
            ResponseStatus.SUCCESS.getMessage(),
            ResponseStatus.SUCCESS.getCode(),
            merchantNotificationConfig
        );
        return new ResponseEntity<>(defaultResponse, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<DefaultResponse> updateMerchantNotificationConfig(
        UpdateMerchantNotificationConfigRequest request,
        String merchantId
    ) {
        log.info(">>> Updating MerchantNotificationConfig with request: {} and code: {}", request, merchantId);
        MerchantNotificationConfig merchantNotificationConfigToUpdate = merchantNotificationConfigRepository
            .findFirstByMerchantId(merchantId)
            .stream()
            .findFirst()
            .orElse(null);
        if (merchantNotificationConfigToUpdate == null) {
            log.info("MerchantNotificationConfig Not found: ");
            throw new NotFoundException(MERCHANT_NOTIFICATION_CONFIG + ResponseStatus.NOT_FOUND.getMessage());
        }

        merchantNotificationConfigToUpdate.setEnableSms(request.getEnableSms());
        merchantNotificationConfigToUpdate.setEnableEmail(request.getEnableEmail());
        merchantNotificationConfigToUpdate.setNotificatonUrl(request.getNotificatonUrl());
        merchantNotificationConfigToUpdate.setUpdatedAt(new Date());

        merchantNotificationConfigRepository.save(merchantNotificationConfigToUpdate);

        DefaultResponse defaultResponse = new DefaultResponse(
            ResponseStatus.SUCCESS.getMessage(),
            ResponseStatus.SUCCESS.getCode(),
            merchantNotificationConfigToUpdate
        );
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }
}
