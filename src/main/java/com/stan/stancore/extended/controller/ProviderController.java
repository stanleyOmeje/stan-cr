package com.stan.stancore.extended.controller;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.dto.auth.MerchantDetailsDto;
import com.systemspecs.remita.enumeration.ServicesEnum;
import com.systemspecs.remita.sdk.auth.CoreSDKAuth;
import com.systemspecs.remita.security.SecurityUtils;
import com.systemspecs.remita.vending.extended.config.VendingCoreProperties;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.ProviderService;
import com.systemspecs.remita.vending.extended.util.RemoteIpHelper;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/vending")
@RestController
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderService providerService;
    private final SecurityUtils securityUtils;
    private final CoreSDKAuth coreSDKAuth;
    private final VendingCoreProperties properties;

    @ActivityTrail
    @GetMapping("/providers")
    public ResponseEntity<DefaultResponse> fetchAllProviders(
        HttpServletRequest request,
        @RequestParam(value = "code", required = false) String code,
        @RequestParam(value = "name", required = false) String name,
        @RequestParam(value = "description", required = false) String description,
        @RequestParam(required = false, defaultValue = "0", value = "page") int page,
        @RequestParam(required = false, defaultValue = "10", value = "pageSize") int pageSize
    ) {
        if (properties.isUseSecretKey()) {
            securityUtils.containsSecretKey(request);
            Optional<MerchantDetailsDto> merchantDetailsDto = getAuthentication(request);
            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("Merchant details not found");
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
        }
        return providerService.fetchAllProviderWithFilter(code, name, description, page, pageSize);
    }

    private Optional<MerchantDetailsDto> getAuthentication(HttpServletRequest httpServletRequest) {
        Optional<MerchantDetailsDto> authResponse = coreSDKAuth.authenticateMerchant(httpServletRequest, ServicesEnum.VENDING);
        if (authResponse.isEmpty()) {
            throw new NotFoundException("Merchant not authenticated");
        }
        MerchantDetailsDto merchantDetails = authResponse.get();
        merchantDetails.setApiKey(httpServletRequest.getHeader("secretKey"));
        return authResponse;
    }

    @ActivityTrail
    @GetMapping("/providers/category/{category}")
    public ResponseEntity<DefaultResponse> fetchProductByCategory(
        HttpServletRequest request,
        @PathVariable("category") String category,
        @PageableDefault Pageable pageable
    ) {
        if (properties.isUseSecretKey()) {
            securityUtils.containsSecretKey(request);
            Optional<MerchantDetailsDto> merchantDetailsDto = getAuthentication(request);
            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("Merchant details not found");
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
        }
        return providerService.fetchProviderByCategory(category, pageable);
    }
}
