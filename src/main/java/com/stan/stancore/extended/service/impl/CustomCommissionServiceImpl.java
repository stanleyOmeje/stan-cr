package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.AlreadyExistException;
import com.systemspecs.remita.vending.extended.exception.BadRequestException;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.CustomCommissionService;
import com.systemspecs.remita.vending.vendingcommon.dto.request.CustomCommissionCreateRequest;
import com.systemspecs.remita.vending.vendingcommon.dto.request.CustomCommissionFilter;
import com.systemspecs.remita.vending.vendingcommon.dto.request.CustomCommissionPage;
import com.systemspecs.remita.vending.vendingcommon.dto.request.CustomCommissionUpdateRequest;
import com.systemspecs.remita.vending.vendingcommon.entity.CustomCommission;
import com.systemspecs.remita.vending.vendingcommon.repository.CustomCommissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomCommissionServiceImpl implements CustomCommissionService {

    private final CustomCommissionRepository customCommissionRepository;

    private final CustomCommissionQueryService commissionQueryService;

    @Override
    public ResponseEntity<DefaultResponse> addCustomCommission(CustomCommissionCreateRequest request) {
        if (Boolean.TRUE.equals(request.getIsPlatformCommission())) {
            validatePlatformCommission(request);
            Optional<CustomCommission> platformCommission = customCommissionRepository.findFirstByProcessorAndIsPlatformCommissionTrue(
                request.getProcessor()
            );
            if (platformCommission.isPresent()) {
                throw new AlreadyExistException("Custom Commission already set up for this processor");
            }
        } else {
            if (StringUtils.isNotBlank(request.getProcessor())) {
                Optional<CustomCommission> optionalCustomCommissionWithProcessor = customCommissionRepository.findFirstByMerchantIdAndProductCodeAndProcessor(
                    request.getMerchantId(),
                    request.getProductCode(),
                    request.getProcessor()
                );
                if (optionalCustomCommissionWithProcessor.isPresent()) {
                    throw new AlreadyExistException("Custom Commission already set up for this processor, merchant and product");
                }
            } else {
                Optional<CustomCommission> customCommission = customCommissionRepository.findFirstByMerchantIdAndProductCodeAndProcessorIsNull(
                    request.getMerchantId(),
                    request.getProductCode()
                );
                if (customCommission.isPresent()) {
                    throw new AlreadyExistException("Custom Commission already set up for this merchant and product");
                }

                log.info("<<<<<Validating Custom commission request{}", request);
                if (StringUtils.isBlank(request.getMerchantId()) || StringUtils.isBlank(request.getProductCode())) {
                    throw new NotFoundException(" Merchant ID and Product code must be provided");
                }
            }
        }
        validateCreateCommission(request);

        log.info(">>>>>Creating Custom Commission");
        CustomCommission commission = getCustomCommission(request);
        customCommissionRepository.save(commission);
        return createSuccessResponse(commission, HttpStatus.CREATED);
    }

    private CustomCommission getCustomCommission(CustomCommissionCreateRequest request) {
        CustomCommission commission = new CustomCommission();
        commission.setIsFixedCommission(request.getIsFixedCommission());
        commission.setFixedCommission(request.getFixedCommission());
        commission.setPercentageCommission(request.getPercentageCommission());
        commission.setMerchantId(request.getMerchantId());
        commission.setIsAppliedCommission(request.getIsAppliedCommission());
        commission.setProductCode(request.getProductCode());
        commission.setPercentageMax(request.getPercentageMax());
        commission.setPercentageMin(request.getPercentageMin());
        commission.setProcessor(request.getProcessor());
        commission.setIsPlatformCommission(request.getIsPlatformCommission());
        commission.setCreatedAt(new Date());
        return commission;
    }

    private void validateCreateCommission(CustomCommissionCreateRequest request) {
        log.info("{-} Validating for update commission with request{}", request);

        validateFixedCommission(request);
        validatePercentageCommission(request);
    }

    private void validateFixedCommission(CustomCommissionCreateRequest request) {
        if (Boolean.TRUE.equals(request.getIsFixedCommission()) && (isZero(request.getFixedCommission()))) {
            throw new BadRequestException(
                "Fixed Commission amount can not be zero or null when isFixedCommission is true",
                ResponseStatus.FAILED_REQUIREMENT.getCode()
            );
        }
    }

    private void validatePercentageCommission(CustomCommissionCreateRequest request) {
        if (
            Boolean.FALSE.equals(request.getIsFixedCommission()) &&
            (isZero(request.getPercentageCommission()) || StringUtils.isBlank(request.getPercentageCommission()))
        ) {
            throw new BadRequestException(
                "Percentage commission can not be zero or blank when isFixedCommission is false",
                ResponseStatus.FAILED_REQUIREMENT.getCode()
            );
        }
        if (!StringUtils.isBlank(request.getPercentageCommission()) && !isZero(request.getPercentageCommission())) {
            validatePercentageRange(request);
        }
    }

    private void validatePercentageRange(CustomCommissionCreateRequest updateRequest) {
        BigDecimal percentageMin = updateRequest.getPercentageMin();
        BigDecimal percentageMax = updateRequest.getPercentageMax();

        if (percentageMin == null && percentageMax == null) {
            return;
        }

        if (percentageMin == null || percentageMax == null) {
            throw new BadRequestException(
                " If one percentage is specified, both PercentageMin and PercentageMax must be provided",
                ResponseStatus.INVALID_PARAMETER.getCode()
            );
        }

        if (percentageMin.compareTo(percentageMax) > 0) {
            throw new BadRequestException(
                "Invalid percentage range: PercentageMin (" + percentageMin + "%) is greater than PercentageMax (" + percentageMax + "%)",
                ResponseStatus.FAILED_REQUIREMENT.getCode()
            );
        }

        if (percentageMin.compareTo(percentageMax) == 0) {
            throw new BadRequestException(
                "PercentageMin and PercentageMax cannot be equal: PercentageMin (" +
                percentageMin +
                "%) and PercentageMax (" +
                percentageMax +
                "%)",
                ResponseStatus.FAILED_REQUIREMENT.getCode()
            );
        }
    }

    private void validatePlatformCommission(CustomCommissionCreateRequest request) {
        log.info("{-} Validating for platform commission with request {}", request);

        if (StringUtils.isBlank(request.getProcessor())) {
            throw new BadRequestException("Processor field cannot be blank", ResponseStatus.FAILED_REQUIREMENT.getCode());
        }

        if (StringUtils.isNotBlank(request.getMerchantId())) {
            throw new BadRequestException("MerchantId is not required", ResponseStatus.FAILED_REQUIREMENT.getCode());
        }

        List<CustomCommission> CommissionList = customCommissionRepository.findAll();
        if (!CommissionList.isEmpty()) {
            log.info("Enter commissionList routine");
            List<CustomCommission> commissionWithProcessorAndMerchantAndProductList = CommissionList
                .stream()
                .filter(commissionWithProcessorAndMerchantAndProduct ->
                    commissionWithProcessorAndMerchantAndProduct.getProcessor() != null &&
                    commissionWithProcessorAndMerchantAndProduct.getMerchantId() != null &&
                    commissionWithProcessorAndMerchantAndProduct.getProductCode() != null
                )
                .collect(Collectors.toList());
            if (!commissionWithProcessorAndMerchantAndProductList.isEmpty()) {
                log.info("commissionWithProcessorAndMerchantAndProductList ...{}", commissionWithProcessorAndMerchantAndProductList.get(0));
                //check if the merchant commission is greater than platform commission
                commissionWithProcessorAndMerchantAndProductList.forEach(commissionWithProcessorAndMerchantAndProduct -> {
                    //check if the processors and product match
                    if (
                        commissionWithProcessorAndMerchantAndProduct.getProcessor().equals(request.getProcessor()) &&
                        commissionWithProcessorAndMerchantAndProduct.getProductCode().equals(request.getProductCode())
                    ) {
                        if (commissionWithProcessorAndMerchantAndProduct.getFixedCommission().compareTo(request.getFixedCommission()) > 0) {
                            throw new BadRequestException(
                                "Configured Merchant Fixed Commission amount is greater than proposed fixed platform commission amount",
                                ResponseStatus.FAILED_REQUIREMENT.getCode()
                            );
                        }
                        if (
                            commissionWithProcessorAndMerchantAndProduct
                                .getPercentageCommission()
                                .compareTo(request.getPercentageCommission()) >
                            0
                        ) {
                            throw new BadRequestException(
                                "Configured Merchant Percentage Commission amount is greater than proposed Percentage platform commission amount",
                                ResponseStatus.FAILED_REQUIREMENT.getCode()
                            );
                        }
                    }
                });
            } else {
                List<CustomCommission> commissionWithMerchantAndProductList = CommissionList
                    .stream()
                    .filter(commissionWithMerchantAndProduct ->
                        commissionWithMerchantAndProduct.getMerchantId() != null &&
                        commissionWithMerchantAndProduct.getProductCode() != null
                    )
                    .collect(Collectors.toList());
                if (!commissionWithMerchantAndProductList.isEmpty()) {
                    log.info("commissionWithMerchantAndProductList...{}", commissionWithMerchantAndProductList.get(0));
                    //check if the merchant commission is greater than platform commission
                    commissionWithMerchantAndProductList.forEach(commissionWithProcessorAndProduct -> {
                        //check if thesame products are involved
                        if (commissionWithProcessorAndProduct.getProductCode().equals(request.getProductCode())) {
                            if (commissionWithProcessorAndProduct.getFixedCommission().compareTo(request.getFixedCommission()) > 0) {
                                throw new BadRequestException(
                                    "Configured Merchant Fixed Commission amount is greater than proposed fixed platform commission amount",
                                    ResponseStatus.FAILED_REQUIREMENT.getCode()
                                );
                            }
                            if (
                                commissionWithProcessorAndProduct.getPercentageCommission().compareTo(request.getPercentageCommission()) > 0
                            ) {
                                throw new BadRequestException(
                                    "Configured Merchant Percentage Commission amount is greater than proposed Percentage platform commission amount",
                                    ResponseStatus.FAILED_REQUIREMENT.getCode()
                                );
                            }
                        }
                    });
                }
            }
        }
    }

    private void validatePlatformCommission(CustomCommissionUpdateRequest request) {
        log.info("{-} Validating for platform commission with request {}", request);

        if (StringUtils.isBlank(request.getProcessor())) {
            throw new BadRequestException("Processor field cannot be blank", ResponseStatus.FAILED_REQUIREMENT.getCode());
        }

        if (StringUtils.isNotBlank(request.getMerchantId())) {
            throw new BadRequestException("MerchantId is not required", ResponseStatus.FAILED_REQUIREMENT.getCode());
        }

        List<CustomCommission> CommissionList = customCommissionRepository.findAll();
        if (!CommissionList.isEmpty()) {
            log.info("Enter commissionList routine");
            List<CustomCommission> commissionWithProcessorAndMerchantAndProductList = CommissionList
                .stream()
                .filter(commissionWithProcessorAndMerchantAndProduct ->
                    commissionWithProcessorAndMerchantAndProduct.getProcessor() != null &&
                    commissionWithProcessorAndMerchantAndProduct.getMerchantId() != null &&
                    commissionWithProcessorAndMerchantAndProduct.getProductCode() != null
                )
                .collect(Collectors.toList());
            if (!commissionWithProcessorAndMerchantAndProductList.isEmpty()) {
                log.info("commissionWithProcessorAndMerchantAndProductList ...{}", commissionWithProcessorAndMerchantAndProductList.get(0));
                //check if the merchant commission is greater than platform commission
                commissionWithProcessorAndMerchantAndProductList.forEach(commissionWithProcessorAndMerchantAndProduct -> {
                    //check if the processors and product match
                    if (
                        commissionWithProcessorAndMerchantAndProduct.getProcessor().equals(request.getProcessor()) &&
                        commissionWithProcessorAndMerchantAndProduct.getProductCode().equals(request.getProductCode())
                    ) {
                        if (commissionWithProcessorAndMerchantAndProduct.getFixedCommission().compareTo(request.getFixedCommission()) > 0) {
                            throw new BadRequestException(
                                "Configured Merchant Fixed Commission amount is greater than proposed fixed platform commission amount",
                                ResponseStatus.FAILED_REQUIREMENT.getCode()
                            );
                        }
                        if (
                            commissionWithProcessorAndMerchantAndProduct
                                .getPercentageCommission()
                                .compareTo(request.getPercentageCommission()) >
                            0
                        ) {
                            throw new BadRequestException(
                                "Configured Merchant Percentage Commission amount is greater than proposed Percentage platform commission amount",
                                ResponseStatus.FAILED_REQUIREMENT.getCode()
                            );
                        }
                    }
                });
            } else {
                List<CustomCommission> commissionWithMerchantAndProductList = CommissionList
                    .stream()
                    .filter(commissionWithMerchantAndProduct ->
                        commissionWithMerchantAndProduct.getMerchantId() != null &&
                        commissionWithMerchantAndProduct.getProductCode() != null
                    )
                    .collect(Collectors.toList());
                if (!commissionWithMerchantAndProductList.isEmpty()) {
                    log.info("commissionWithMerchantAndProductList...{}", commissionWithMerchantAndProductList.get(0));
                    //check if the merchant commission is greater than platform commission
                    commissionWithMerchantAndProductList.forEach(commissionWithProcessorAndProduct -> {
                        //check if thesame products are involved
                        if (commissionWithProcessorAndProduct.getProductCode().equals(request.getProductCode())) {
                            if (commissionWithProcessorAndProduct.getFixedCommission().compareTo(request.getFixedCommission()) > 0) {
                                throw new BadRequestException(
                                    "Configured Merchant Fixed Commission amount is greater than proposed fixed platform commission amount",
                                    ResponseStatus.FAILED_REQUIREMENT.getCode()
                                );
                            }
                            if (
                                commissionWithProcessorAndProduct.getPercentageCommission().compareTo(request.getPercentageCommission()) > 0
                            ) {
                                throw new BadRequestException(
                                    "Configured Merchant Percentage Commission amount is greater than proposed Percentage platform commission amount",
                                    ResponseStatus.FAILED_REQUIREMENT.getCode()
                                );
                            }
                        }
                    });
                }
            }
        }
    }

    @Override
    public ResponseEntity<DefaultResponse> updateCustomCommission(CustomCommissionUpdateRequest commissionUpdateRequest, Long id) {
        log.debug("Processing commission update request for commissionId: {}", id);
        CustomCommission existingCommission = customCommissionRepository
            .findFirstById(id)
            .orElseThrow(() -> new NotFoundException(("Commission with Id not found")));
        if (Boolean.TRUE.equals(commissionUpdateRequest.getIsPlatformCommission())) {
            validatePlatformCommission(commissionUpdateRequest);
        }
        validateUpdateCommission(commissionUpdateRequest);

        existingCommission.setIsFixedCommission(commissionUpdateRequest.getIsFixedCommission());
        existingCommission.setFixedCommission(commissionUpdateRequest.getFixedCommission());
        existingCommission.setPercentageCommission(commissionUpdateRequest.getPercentageCommission());
        existingCommission.setIsAppliedCommission(commissionUpdateRequest.getIsAppliedCommission());
        existingCommission.setPercentageMin(commissionUpdateRequest.getPercentageMin());
        existingCommission.setPercentageMax(commissionUpdateRequest.getPercentageMax());
        existingCommission.setUpdatedAt(new Date());
        customCommissionRepository.save(existingCommission);
        return createSuccessResponse(existingCommission, HttpStatus.OK);
    }

    private void validateUpdateCommission(CustomCommissionUpdateRequest request) {
        log.info("{-} Validating for update commission with request{}", request);

        validateFixedCommission(request);
        validatePercentageCommission(request);
    }

    private void validateFixedCommission(CustomCommissionUpdateRequest request) {
        if (Boolean.TRUE.equals(request.getIsFixedCommission()) && (isZero(request.getFixedCommission()))) {
            throw new BadRequestException(
                "Fixed Commission amount can not be zero or null when isFixedCommission is true",
                ResponseStatus.FAILED_REQUIREMENT.getCode()
            );
        }
    }

    private void validatePercentageCommission(CustomCommissionUpdateRequest request) {
        if (
            Boolean.FALSE.equals(request.getIsFixedCommission()) &&
            (isZero(request.getPercentageCommission()) || StringUtils.isBlank(request.getPercentageCommission()))
        ) {
            throw new BadRequestException(
                "Percentage commission not be zero or blank when isFixedCommission is false",
                ResponseStatus.FAILED_REQUIREMENT.getCode()
            );
        }
        if (!StringUtils.isBlank(request.getPercentageCommission()) && !isZero(request.getPercentageCommission())) {
            validatePercentageRange(request);
        }
    }

    private void validatePercentageRange(CustomCommissionUpdateRequest updateRequest) {
        BigDecimal percentageMin = updateRequest.getPercentageMin();
        BigDecimal percentageMax = updateRequest.getPercentageMax();

        if (percentageMin == null && percentageMax == null) {
            return;
        }

        if (percentageMin == null || percentageMax == null) {
            throw new BadRequestException(
                "If one percentage is specified, both PercentageMin and PercentageMax must be provided",
                ResponseStatus.INVALID_PARAMETER.getCode()
            );
        }

        if (percentageMin.compareTo(percentageMax) > 0) {
            throw new BadRequestException(
                " Invalid percentage range: PercentageMin (" + percentageMin + "%) is greater than PercentageMax (" + percentageMax + "%)",
                ResponseStatus.FAILED_REQUIREMENT.getCode()
            );
        }

        if (percentageMin.compareTo(percentageMax) == 0) {
            throw new BadRequestException(
                "PercentageMin and PercentageMax cannot be equal: PercentageMin (" +
                percentageMin +
                "%) and PercentageMax (" +
                percentageMax +
                "%)",
                ResponseStatus.FAILED_REQUIREMENT.getCode()
            );
        }
    }

    @Override
    public ResponseEntity<DefaultResponse> getAllCommissionsWithFilter(
        String merchantId,
        String productCode,
        String processor,
        int page,
        int pageSize
    ) {
        log.info("{-} Fetching all Commission using filter");
        CustomCommissionFilter filter = new CustomCommissionFilter();
        filter.setMerchantId(merchantId);
        filter.setProductCode(productCode);
        filter.setProcessor(processor);

        CustomCommissionPage commissionPage = new CustomCommissionPage();
        commissionPage.setPageSize(pageSize);
        commissionPage.setPageNo(page);

        Page<CustomCommission> customCommissions = commissionQueryService.getAllCommissionWithFilter(commissionPage, filter);

        DefaultResponse defaultResponse = new DefaultResponse();
        if (commissionPage.getPageSize() > 50) {
            defaultResponse.setMessage("{-} Maximum page size exceeded");
            defaultResponse.setStatus(ResponseStatus.FAILED_REQUIREMENT.getCode());
        }

        Map<String, Object> map = new HashMap<>();
        map.put("totalPages", customCommissions.getTotalPages());
        map.put(" totalContent", customCommissions.getTotalElements());
        map.put("items", customCommissions.getContent());
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setData(map);
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    private ResponseEntity<DefaultResponse> createSuccessResponse(CustomCommission commission, HttpStatus status) {
        DefaultResponse response = new DefaultResponse();
        response.setStatus(ResponseStatus.SUCCESS.getCode());
        response.setMessage(ResponseStatus.SUCCESS.getMessage());
        response.setData(commission);
        return new ResponseEntity<>(response, status);
    }

    private boolean isZero(String str) {
        if (str == null || str.trim().isEmpty()) {
            return true;
        }

        String trimmed = str.trim();

        // Check if it's "0"
        if (trimmed.equals("0")) {
            return true;
        }

        // Check if it's a decimal representation of zero
        try {
            double value = Double.parseDouble(trimmed);
            return value == 0.0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isZero(BigDecimal value) {
        if (value == null) {
            return true;
        }
        return value.compareTo(BigDecimal.ZERO) == 0;
    }
}
