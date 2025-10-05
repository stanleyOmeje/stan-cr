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
import com.systemspecs.remita.vending.extended.service.TransactionService;
import com.systemspecs.remita.vending.extended.util.IPUtil;
import com.systemspecs.remita.vending.extended.util.MerchantDetailsUtil;
import com.systemspecs.remita.vending.extended.util.RemoteIpHelper;
import com.systemspecs.remita.vending.vendingcommon.dto.request.*;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/vending/transactions")
@RestController
public class TransactionController {

    private final TransactionService transactionService;
    private final SecurityUtils securityUtils;
    private final CoreSDKAuth coreSDKAuth;
    private final VendingCoreProperties properties;
    private final MerchantDetailsUtil merchantDetailsUtil;

    public TransactionController(
        TransactionService transactionService,
        SecurityUtils securityUtils,
        CoreSDKAuth coreSDKAuth,
        VendingCoreProperties properties,
        MerchantDetailsUtil merchantDetailsUtil
    ) {
        this.transactionService = transactionService;
        this.securityUtils = securityUtils;
        this.coreSDKAuth = coreSDKAuth;
        this.properties = properties;
        this.merchantDetailsUtil = merchantDetailsUtil;
    }

    @ActivityTrail
    @PostMapping
    public ResponseEntity<DefaultResponse> transact(HttpServletRequest request, @RequestBody @Valid TransactionRequest transactionRequest) {
        long startTime = System.currentTimeMillis();
        log.info(
            ">>> [START] TransactionController::performTransaction | Duration={}ms | Request={} | clientReference={} | paymentIdentifier={}",
            transactionRequest,
            startTime,
            transactionRequest.getClientReference(),
            transactionRequest.getPaymentIdentifier()
        );

        DefaultResponse defaultResponse = new DefaultResponse();
        try {
            // Validate security
            securityUtils.containsSecretKey(request);

            // Authenticate merchant
            Optional<MerchantDetailsDto> merchantDetailsDto = getAuthentication(request);
            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("Merchant details not found");
            }

            // Attach request IP
            String merchantIp = resolveIp(request);
            merchantDetailsDto.get().setRequestIp(merchantIp);

            // Perform transaction
            ResponseEntity<DefaultResponse> performTransaction = transactionService.performTransaction(
                transactionRequest,
                merchantDetailsDto.get()
            );

            long duration = System.currentTimeMillis() - startTime;
            log.info(
                ">>> [END] TransactionController::performTransaction | Duration={}ms | Response={} | clientReference={} | paymentIdentifier={}",
                duration,
                performTransaction.getBody(),
                transactionRequest.getClientReference(),
                transactionRequest.getPaymentIdentifier()
            );

            return performTransaction;
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (NotFoundException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            defaultResponse.setStatus(TransactionStatus.AUTHENTICATION_ERROR.getCode());
            defaultResponse.setMessage(TransactionStatus.AUTHENTICATION_ERROR.getMessage());
            log.error("Unexpected error occurred in performTransaction", e);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info(
            ">>> [END] TransactionController::performTransaction | Duration={}ms | Response={} | clientReference={} | paymentIdentifier={}",
            duration,
            defaultResponse,
            transactionRequest.getClientReference(),
            transactionRequest.getPaymentIdentifier()
        );

        return ResponseEntity.ok(defaultResponse);
    }

    public String resolveIp(HttpServletRequest request) {
        String merchantIp = RemoteIpHelper.getRemoteIpFrom(request);
        log.info("merchantIp: {}", merchantIp);
        if (merchantIp.contains(",")) {
            String[] duplicateIp = merchantIp.split(",");
            merchantIp = duplicateIp[0];
            log.info("Split merchantIp address is ...{}", merchantIp);
        }
        return merchantIp;
    }

    @ActivityTrail
    @PostMapping(value = "/v2")
    public ResponseEntity<DefaultResponse> newTransact(
        HttpServletRequest request,
        @RequestBody @Valid TransactionRequest transactionRequest
    ) {
        log.info(">>> Inside Transaction Controller::transact with Request ...{}", transactionRequest);

        Optional<MerchantDetailsDto> merchantDetailsOpt = merchantDetailsUtil.getMerchantDetails(request);
        if (merchantDetailsOpt.isEmpty()) {
            throw new NotFoundException("Merchant details not found in request header.");
        }
        return transactionService.performTransactionV2(transactionRequest, merchantDetailsOpt.get());
    }

    @ActivityTrail
    @GetMapping
    public ResponseEntity<DefaultResponse> fetchAllTransactions(
        HttpServletRequest request,
        @RequestParam(required = false) String productCode,
        @RequestParam(required = false) TransactionStatus status,
        @RequestParam(required = false) String merchantOrgId,
        @RequestParam(required = false) String ipAddress,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") String internalReference,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") String categoryCode,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") String clientReference,
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
                merchantOrgId = merchantDetailsDto.get().getOrgId();
            } else {
                String merchantIp = IPUtil.getClientIp(request);
                log.info("MerchantIp ...{}", merchantIp);
                if (merchantIp.contains(",")) {
                    String[] duplicateIp = merchantIp.split(",");
                    merchantIp = duplicateIp[0];
                    ipAddress = merchantIp;
                    log.info("Splited merchantIp is ...{}", merchantIp);
                }
                String[] permittedIp = properties.getAllowedIP().split(",");
                if (!Arrays.asList(permittedIp).contains(merchantIp)) {
                    throw new NotFoundException("Merchant IP not permitted");
                }
            }
            TransactionSearchCriteria criteria = getTransactionSearchCriteria(
                productCode,
                status,
                start,
                end,
                internalReference,
                categoryCode,
                clientReference,
                merchantOrgId,
                ipAddress
            );
            TransactionPage transactionPage = new TransactionPage();
            transactionPage.setPageNo(page);
            transactionPage.setPageSize(pageSize);

            return transactionService.fetchAllTransaction(transactionPage, criteria);
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

    private TransactionSearchCriteria getTransactionSearchCriteria(
        String productCode,
        TransactionStatus status,
        LocalDate start,
        LocalDate end,
        String internalReference,
        String categoryCode,
        String clientReference,
        String merchantOrgId,
        String ipAddress
    ) {
        TransactionSearchCriteria criteria = new TransactionSearchCriteria();
        criteria.setProductCode(productCode);
        criteria.setStatus(status);
        criteria.setStartDate(start);
        criteria.setEndDate(end);
        criteria.setInternalReference(internalReference);
        criteria.setCategoryCode(categoryCode);
        criteria.setClientReference(clientReference);
        if (StringUtils.isNotBlank(ipAddress)) {
            criteria.setIpAddress(ipAddress);
        } else if (StringUtils.isNotBlank(merchantOrgId)) {
            criteria.setUserId(merchantOrgId);
        }
        return criteria;
    }

    @ActivityTrail
    @GetMapping("/{reference}")
    public ResponseEntity<DefaultResponse> getTransactionByReference(HttpServletRequest request, @PathVariable String reference) {
        log.info("inside getTransactionByReference controller with reference {}", reference);
        DefaultResponse defaultResponse = new DefaultResponse();
        try {
            securityUtils.containsSecretKey(request);
            Optional<MerchantDetailsDto> merchantDetailsDto = getAuthentication(request);

            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("Merchant details not found");
            }
            String merchantOrgId = merchantDetailsDto.get().getOrgId();
            return transactionService.getTransactionByPaymentIdentifier(reference, merchantOrgId);
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
    @GetMapping("/bulk")
    public ResponseEntity<DefaultResponse> getBulkTransactionByClientReference(
        HttpServletRequest request,
        @RequestParam(required = false) String productCode,
        @RequestParam(required = false) String categoryCode,
        @RequestParam(required = false) String accountNumber,
        @RequestParam(required = false) String phoneNumber,
        @RequestParam(required = false) String bulkClientReference,
        @RequestParam(required = false) String vendStatus,
        @RequestParam(required = false, defaultValue = "0", value = "page") int page,
        @RequestParam(required = false, defaultValue = "10", value = "pageSize") int pageSize
    ) {
        log.info("Inside getBulkTransactionByClientReference Controller with client reference...{}", bulkClientReference);
        if (properties.isUseSecretKey()) {
            securityUtils.containsSecretKey(request);
            Optional<MerchantDetailsDto> merchantDetailsDto = getAuthentication(request);

            if (merchantDetailsDto.isEmpty()) {
                log.error("Merchant details not found");
                throw new NotFoundException("Merchant details not found");
            }
        } else {
            String merchantIp = IPUtil.getClientIp(request);
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
        BulkTransactionSearchCriteria bulkTransactionSearchCriteria = getBulkTransactionSearchCriteria(
            productCode,
            categoryCode,
            accountNumber,
            phoneNumber,
            bulkClientReference,
            vendStatus
        );
        BulkTransactionPage bulkTransactionPage = getBulkTransactionPage(page, pageSize);

        return transactionService.getBulkTransactionByClientReference(bulkTransactionPage, bulkTransactionSearchCriteria);
    }

    BulkTransactionSearchCriteria getBulkTransactionSearchCriteria(
        String productCode,
        String categoryCode,
        String accountNumber,
        String phoneNumber,
        String bulkClientReference,
        String vendStatus
    ) {
        BulkTransactionSearchCriteria bulkTransactionSearchCriteria = new BulkTransactionSearchCriteria();
        bulkTransactionSearchCriteria.setProductCode(productCode);
        bulkTransactionSearchCriteria.setCategoryCode(categoryCode);
        bulkTransactionSearchCriteria.setAccountNumber(accountNumber);
        bulkTransactionSearchCriteria.setPhoneNumber(phoneNumber);
        bulkTransactionSearchCriteria.setBulkClientReference(bulkClientReference);
        bulkTransactionSearchCriteria.setVendStatus(vendStatus);

        return bulkTransactionSearchCriteria;
    }

    BulkTransactionPage getBulkTransactionPage(int pageNo, int pageSize) {
        BulkTransactionPage bulkTransactionPage = new BulkTransactionPage();
        bulkTransactionPage.setPageNo(pageNo);
        bulkTransactionPage.setPageSize(pageSize);

        return bulkTransactionPage;
    }

    private Optional<MerchantDetailsDto> getAuthentication(HttpServletRequest request) {
        try {
            Optional<MerchantDetailsDto> authResponse = coreSDKAuth.authenticateMerchant(request, ServicesEnum.VENDING);
            if (authResponse.isEmpty()) {
                throw new BadRequestException(
                    TransactionStatus.AUTHENTICATION_ERROR.getMessage(),
                    TransactionStatus.AUTHENTICATION_ERROR.getCode()
                );
            }
            return authResponse;
        } catch (Exception ex) {
            log.error("Error during merchant authentication", ex);
            throw new BadRequestException(
                TransactionStatus.AUTHENTICATION_ERROR.getMessage(),
                TransactionStatus.AUTHENTICATION_ERROR.getCode()
            );
        }
    }
}
