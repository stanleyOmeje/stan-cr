package com.stan.stancore.extended.controller;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.dto.auth.MerchantDetailsDto;
import com.systemspecs.remita.enumeration.ServicesEnum;
import com.systemspecs.remita.sdk.auth.CoreSDKAuth;
import com.systemspecs.remita.security.SecurityUtils;
import com.systemspecs.remita.vending.extended.config.VendingCoreProperties;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.exception.BadRequestException;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.ProductService;
import com.systemspecs.remita.vending.extended.util.RemoteIpHelper;
import com.systemspecs.remita.vending.vendingcommon.enums.ProductType;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
public class ProductController {

    private final ProductService productService;
    private final SecurityUtils securityUtils;
    private final CoreSDKAuth coreSDKAuth;
    private final VendingCoreProperties properties;

    public ProductController(
        ProductService productService,
        SecurityUtils securityUtils,
        CoreSDKAuth coreSDKAuth,
        VendingCoreProperties properties
    ) {
        this.productService = productService;
        this.securityUtils = securityUtils;
        this.coreSDKAuth = coreSDKAuth;
        this.properties = properties;
    }

    @ActivityTrail
    @GetMapping("/products")
    public ResponseEntity<DefaultResponse> fetchAllProduct(
        HttpServletRequest request,
        @RequestParam(value = "countryCode", required = false) String countryCode,
        @RequestParam(value = "country", required = false) String country,
        @RequestParam(value = "productType", required = false) ProductType productType,
        @RequestParam(required = false, value = "categoryCode") String categoryCode,
        @RequestParam(required = false, value = "currencyCode") String currencyCode,
        @RequestParam(required = false, value = "provider") String provider,
        @RequestParam(required = false, value = "code") String code,
        @RequestParam(required = false, defaultValue = "0", value = "page") int page,
        @RequestParam(required = false, defaultValue = "10", value = "pageSize") int pageSize
    ) {
        DefaultResponse defaultResponse = new DefaultResponse();
        try {
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
            return productService.fetchAllProductWithFilter(
                categoryCode,
                currencyCode,
                country,
                countryCode,
                productType,
                provider,
                code,
                page,
                pageSize
            );
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

    @ActivityTrail
    @GetMapping("/products/{code}")
    public ResponseEntity<DefaultResponse> fetchProductByCode(HttpServletRequest request, @PathVariable("code") String code) {
        DefaultResponse defaultResponse = new DefaultResponse();
        try {
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
            return productService.fetchProductByCode(code);
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

    @ActivityTrail
    @GetMapping("/products/category/{category}")
    public ResponseEntity<DefaultResponse> fetchProductByCategory(
        @PathVariable("category") String category,
        @PageableDefault Pageable pageable
    ) {
        return productService.fetchProductByCategory(category, pageable);
    }

    private Optional<MerchantDetailsDto> getAuthentication(HttpServletRequest httpServletRequest) {
        Optional<MerchantDetailsDto> authResponse = coreSDKAuth.authenticateMerchant(httpServletRequest, ServicesEnum.VENDING);
        if (authResponse.isEmpty()) {
            throw new BadRequestException(
                TransactionStatus.AUTHENTICATION_ERROR.getMessage(),
                TransactionStatus.AUTHENTICATION_ERROR.getCode()
            );
        }
        MerchantDetailsDto merchantDetails = authResponse.get();
        merchantDetails.setApiKey(httpServletRequest.getHeader("secretKey"));
        return authResponse;
    }
}
