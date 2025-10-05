package com.stan.stancore.extended.controller;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.dto.auth.MerchantDetailsDto;
import com.systemspecs.remita.enumeration.ServicesEnum;
import com.systemspecs.remita.sdk.auth.CoreSDKAuth;
import com.systemspecs.remita.security.SecurityUtils;
import com.systemspecs.remita.vending.extended.config.VendingCoreProperties;
import com.systemspecs.remita.vending.extended.dto.request.ValidationRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.exception.BadRequestException;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.ValidationService;
import com.systemspecs.remita.vending.extended.util.RemoteIpHelper;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/vending/validate")
@RestController
public class ValidationController {

    private final ValidationService validationService;
    private final SecurityUtils securityUtils;
    private final CoreSDKAuth coreSDKAuth;
    private final VendingCoreProperties properties;

    public ValidationController(
        ValidationService validationService,
        SecurityUtils securityUtils,
        CoreSDKAuth coreSDKAuth,
        VendingCoreProperties properties
    ) {
        this.validationService = validationService;
        this.securityUtils = securityUtils;
        this.coreSDKAuth = coreSDKAuth;
        this.properties = properties;
    }

    @ActivityTrail
    @PostMapping
    public ResponseEntity<DefaultResponse> validate(HttpServletRequest request, @RequestBody @Valid ValidationRequest validationRequest) {
        log.info("inside validate method");
        DefaultResponse defaultResponse = new DefaultResponse();
        try {
            if (properties.isUseSecretKey()) {
                securityUtils.containsSecretKey(request);
                Optional<MerchantDetailsDto> merchantDetailsDto = getAuthentication(request);
                if (merchantDetailsDto.isEmpty()) {
                    throw new NotFoundException("Merchant details not found");
                }
            } else {
                log.info("inside else block");
                String merchantIp = RemoteIpHelper.getRemoteIpFrom(request);
                log.info("MerchantIp ...{}", merchantIp);
                if (merchantIp.contains(",")) {
                    String[] duplicateIp = merchantIp.split(",");
                    merchantIp = duplicateIp[0];
                    log.info("Splited merchantIp is ...{}", merchantIp);
                }
                String configureIp = properties.getAllowedIP();
                if (StringUtils.isBlank(configureIp)) {
                    throw new NotFoundException("Configured IP is empty");
                }
                log.info("configureIp is ...{}", configureIp);
                String[] permittedIp = properties.getAllowedIP().split(",");
                if (!Arrays.asList(permittedIp).contains(merchantIp)) {
                    throw new NotFoundException("Merchant IP not permitted");
                }
            }
            return validationService.validateAccount(validationRequest);
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (NotFoundException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            defaultResponse.setStatus(TransactionStatus.AUTHENTICATION_ERROR.getCode());
            defaultResponse.setMessage(TransactionStatus.AUTHENTICATION_ERROR.getMessage());
            e.printStackTrace();
        }
        return ResponseEntity.ok(defaultResponse);
    }

    private Optional<MerchantDetailsDto> getAuthentication(HttpServletRequest httpServletRequest) {
        Optional<MerchantDetailsDto> authResponse = coreSDKAuth.authenticateMerchant(httpServletRequest, ServicesEnum.VENDING);
        if (authResponse.isEmpty()) {
            throw new NotFoundException("Merchant not authenticated");
        }
        return authResponse;
    }
}
