package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.extended.utils.RedisUtility;
import com.systemspecs.remita.vending.extended.dto.request.VendingRouteConfigRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.enums.Processors;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.AlreadyExistException;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.VendingRouteConfigService;
import com.systemspecs.remita.vending.vendingcommon.entity.VendingServiceRouteConfig;
import com.systemspecs.remita.vending.vendingcommon.repository.VendingServiceRouteConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VendingRouteConfigServiceImp implements VendingRouteConfigService {

    private final VendingServiceRouteConfigRepository vendingServiceRouteConfigRepository;
    private final RedisUtility redisUtility;

    private static final String CACHE_PREFIX = "ROUTE_CONFIG:";
    private static final int CACHE_TTL_SECONDS = 1800; // 30 minutes

    public VendingRouteConfigServiceImp(
        VendingServiceRouteConfigRepository vendingServiceRouteConfigRepository,
        RedisUtility redisUtility
    ) {
        this.vendingServiceRouteConfigRepository = vendingServiceRouteConfigRepository;
        this.redisUtility = redisUtility;
    }

    @Override
    public ResponseEntity<DefaultResponse> createVendingRoute(VendingRouteConfigRequest request) {
        log.info(">>> Creating VendingRoute with: {}", request);
        String code = request.getProductCode().replaceAll("\\s+", "-");
        validateProcessorId(request.getProcessorId());

        if (request.getEnableFallbackProcessor()) {
            validateProcessorId(request.getFallbackProcessorId());
        }

        Optional<VendingServiceRouteConfig> vendingRouteConfigCheck =
            vendingServiceRouteConfigRepository.findByProductCode(request.getProductCode());
        if (vendingRouteConfigCheck.isPresent()) {
            throw new AlreadyExistException("vendingRouteConfig " + ResponseStatus.ALREADY_EXIST.getMessage());
        }

        VendingServiceRouteConfig vendingServiceRouteConfig = new VendingServiceRouteConfig();
        vendingServiceRouteConfig.setProductCode(request.getProductCode());
        vendingServiceRouteConfig.setProcessorId(request.getProcessorId());
        vendingServiceRouteConfig.setActive(request.getActive());
        vendingServiceRouteConfig.setEnableFallBackProcessor(request.getEnableFallbackProcessor());
        vendingServiceRouteConfig.setFallBackProcessorId(request.getFallbackProcessorId());
        vendingServiceRouteConfig.setSystemProductType(request.getSystemProductType());

        vendingServiceRouteConfig = vendingServiceRouteConfigRepository.save(vendingServiceRouteConfig);

        // cache single route-by-product
        String cacheKey = CACHE_PREFIX + vendingServiceRouteConfig.getProductCode();
        try {
            redisUtility.saveObjectToRedis(cacheKey, vendingServiceRouteConfig, CACHE_TTL_SECONDS);
            log.debug("Cached route config in Redis: {}", cacheKey);
        } catch (Exception redisEx) {
            log.warn("Failed to cache route config for {}: {}", vendingServiceRouteConfig.getProductCode(), redisEx.getMessage());
        }

        DefaultResponse defaultResponse = new DefaultResponse(
            ResponseStatus.SUCCESS.getMessage(),
            ResponseStatus.SUCCESS.getCode(),
            vendingServiceRouteConfig
        );
        return new ResponseEntity<>(defaultResponse, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<DefaultResponse> updateVendingRoute(VendingRouteConfigRequest request, String productCode) {
        log.info("inside updateVendingRoute");
        validateProcessorId(request.getProcessorId());

        if (request.getEnableFallbackProcessor()) {
            validateProcessorId(request.getFallbackProcessorId());
        }

        VendingServiceRouteConfig vendingServiceRouteConfig = vendingServiceRouteConfigRepository
            .findByProductCode(productCode)
            .orElseThrow(() -> new NotFoundException("vendingServiceRouteConfig " + ResponseStatus.NOT_FOUND.getMessage()));

        vendingServiceRouteConfig.setProcessorId(request.getProcessorId());
        vendingServiceRouteConfig.setActive(request.getActive());
        vendingServiceRouteConfig.setEnableFallBackProcessor(request.getEnableFallbackProcessor());
        vendingServiceRouteConfig.setFallBackProcessorId(request.getFallbackProcessorId());
        vendingServiceRouteConfig.setUpdatedDate(new Date());
        vendingServiceRouteConfig.setSystemProductType(request.getSystemProductType());

        vendingServiceRouteConfig = vendingServiceRouteConfigRepository.save(vendingServiceRouteConfig);

        // refresh cache for this route
        String cacheKey = CACHE_PREFIX + vendingServiceRouteConfig.getProductCode();
        try {
            redisUtility.saveObjectToRedis(cacheKey, vendingServiceRouteConfig, CACHE_TTL_SECONDS);
            log.debug("Updated cached route config in Redis: {}", cacheKey);
        } catch (Exception redisEx) {
            log.warn("Failed to update cache for {}: {}", vendingServiceRouteConfig.getProductCode(), redisEx.getMessage());
        }

        DefaultResponse defaultResponse = new DefaultResponse(
            ResponseStatus.SUCCESS.getMessage(),
            ResponseStatus.SUCCESS.getCode(),
            vendingServiceRouteConfig
        );
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DefaultResponse> fetchAllVendingRoute(Pageable pageable) {
        DefaultResponse defaultResponse = new DefaultResponse();
        Page<VendingServiceRouteConfig> vendingServiceRouteConfigs = vendingServiceRouteConfigRepository.findAll(pageable);

        Map<String, Object> map = new HashMap<>();
        map.put("totalPage", vendingServiceRouteConfigs.getTotalPages());
        map.put("totalContent", vendingServiceRouteConfigs.getTotalElements());
        map.put("items", vendingServiceRouteConfigs.getContent());
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setData(map);

        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DefaultResponse> deleteVendingRouteByProductCode(String productCode) {
        log.info(">>> deleting VendingRoute by productCode: {}", productCode);
        VendingServiceRouteConfig vendingServiceRouteConfig = vendingServiceRouteConfigRepository
            .findByProductCodeAndActiveTrue(productCode)
            .orElseThrow(() -> new NotFoundException("vendingServiceRouteConfig " + ResponseStatus.NOT_FOUND.getMessage()));
        vendingServiceRouteConfigRepository.delete(vendingServiceRouteConfig);

        // invalidate cache for this product
        String cacheKey = CACHE_PREFIX + productCode;
        try {
            redisUtility.deleteObjectFromRedis(cacheKey);
            log.debug("Deleted cache key: {}", cacheKey);
        } catch (Exception redisEx) {
            log.warn("Failed to delete cache key {}: {}", cacheKey, redisEx.getMessage());
        }

        DefaultResponse defaultResponse = new DefaultResponse(ResponseStatus.SUCCESS.getMessage(), ResponseStatus.SUCCESS.getCode());
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    private static void validateProcessorId(String processor) {
        boolean validProcessorId = Arrays.stream(Processors.values()).map(Enum::toString).collect(Collectors.toList()).contains(processor);
        if (!validProcessorId) {
            throw new NotFoundException("processor " + ResponseStatus.NOT_FOUND.getMessage());
        }
    }

    @Override
    public ResponseEntity<DefaultResponse> getRouteConfigByProductCode(String productCode) {
        log.info(">>> Fetching Route by code {}", productCode);
        String cacheKey = CACHE_PREFIX + productCode;

        // try redis first
        try {
            VendingServiceRouteConfig cached = redisUtility.getObjectFromRedis(cacheKey, VendingServiceRouteConfig.class);
            if (cached != null) {
                log.debug("[getRouteConfigByProductCode] Cache hit for {}", cacheKey);
                DefaultResponse defaultResponse = new DefaultResponse(ResponseStatus.SUCCESS.getMessage(), ResponseStatus.SUCCESS.getCode(), cached);
                return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
            }
        } catch (Exception redisEx) {
            log.warn("[getRouteConfigByProductCode] Redis lookup failed for {} | Reason={}", cacheKey, redisEx.getMessage());
        }

        // fallback to DB
        VendingServiceRouteConfig vendingServiceRouteConfig = vendingServiceRouteConfigRepository
            .findByProductCode(productCode)
            .orElseThrow(() -> new NotFoundException("vendingServiceRouteConfig " + ResponseStatus.NOT_FOUND.getMessage()));

        // cache the DB result (best-effort)
        try {
            redisUtility.saveObjectToRedis(cacheKey, vendingServiceRouteConfig, CACHE_TTL_SECONDS);
        } catch (Exception redisEx) {
            log.warn("[getRouteConfigByProductCode] Failed to cache result for {}: {}", cacheKey, redisEx.getMessage());
        }

        DefaultResponse defaultResponse = new DefaultResponse(
            ResponseStatus.SUCCESS.getMessage(),
            ResponseStatus.SUCCESS.getCode(),
            vendingServiceRouteConfig
        );
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }
}
