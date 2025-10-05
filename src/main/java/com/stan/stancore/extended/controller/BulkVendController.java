package com.stan.stancore.extended.controller;

import com.google.gson.Gson;
import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.dto.auth.MerchantDetailsDto;
import com.systemspecs.remita.enumeration.ServicesEnum;
import com.systemspecs.remita.sdk.auth.CoreSDKAuth;
import com.systemspecs.remita.security.SecurityUtils;
import com.systemspecs.remita.vending.extended.config.VendingCoreProperties;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.BulkVendingService;
import com.systemspecs.remita.vending.extended.util.RemoteIpHelper;
import com.systemspecs.remita.vending.vendingcommon.dto.request.BulkVendingRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;

@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/vending/transactions")
@RestController
@RequiredArgsConstructor
@Slf4j
public class BulkVendController {

    private final BulkVendingService bulkVendingService;
    private final SecurityUtils securityUtils;
    private final CoreSDKAuth coreSDKAuth;
    private final VendingCoreProperties properties;

    @ActivityTrail
    @PostMapping(value = "bulk-vend")
    public ResponseEntity<DefaultResponse> bulkPurchaseData(@RequestBody String payload, HttpServletRequest request) {
        Gson gson = new Gson();
        String profileId = null;
        Optional<MerchantDetailsDto> merchantDetailsDto = Optional.of(new MerchantDetailsDto());
        if (properties.isUseSecretKey()) {
            securityUtils.containsSecretKey(request);
            merchantDetailsDto = getAuthentication(request);
            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("MerchantDetails not found");
            }
        } else {
            String merchantIp = RemoteIpHelper.getRemoteIpFrom(request);
            log.info("MerchantIp ...{}", merchantIp);
            if (merchantIp.contains(",")) {
                String[] duplicateIp = merchantIp.split(",");
                merchantIp = duplicateIp[0];
                log.info("Splited merchantIp is ...{}", merchantIp);
            }
            String configureIp = properties.getAllowedIP();
            if (StringUtils.isBlank(configureIp)) {
                throw new NotFoundException("Confired IP is empty");
            }
            log.info("configureIp is ...{}", configureIp);
            String[] permittedIp = properties.getAllowedIP().split(",");
            if (!Arrays.asList(permittedIp).contains(merchantIp)) {
                throw new NotFoundException("Merchant IP not permitted");
            }
            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("Merchant details is not found");
            }
            merchantDetailsDto.get().setRequestIp(merchantIp);
        }
        profileId = merchantDetailsDto.get().getOrgId();
        BulkVendingRequest bulkVendingRequest = gson.fromJson(payload, BulkVendingRequest.class);
        bulkVendingRequest.setProfileId(profileId);

        return bulkVendingService.processBulkVending(bulkVendingRequest, merchantDetailsDto.get());
    }

    private Optional<MerchantDetailsDto> getAuthentication(HttpServletRequest request) {
        Optional<MerchantDetailsDto> authResponse = coreSDKAuth.authenticateMerchant(request, ServicesEnum.VENDING);
        if (authResponse.isEmpty()) {
            throw new NotFoundException("Merchant not authenticated");
        }
        return authResponse;
    }
}
