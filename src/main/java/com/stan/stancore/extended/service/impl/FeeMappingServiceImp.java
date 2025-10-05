package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.extended.dto.request.FeeMappingRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.FeeMappingService;
import com.systemspecs.remita.vending.vendingcommon.entity.FeeMapping;
import com.systemspecs.remita.vending.vendingcommon.repository.FeeMappingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Slf4j
public class FeeMappingServiceImp implements FeeMappingService {
    private final FeeMappingRepository feeMappingRepository;

    public FeeMappingServiceImp(FeeMappingRepository feeMappingRepository) {
        this.feeMappingRepository = feeMappingRepository;
    }

    @Override
    public ResponseEntity<DefaultResponse> updateFeeMapping(FeeMappingRequest feeMappingRequest, String productCode) {
        log.info(">>> Updating updateFeeMapping with feeMappingRequest: {} and productCode: {}",feeMappingRequest,productCode);
        FeeMapping feeMapping = feeMappingRepository.findFirstByProductCode(productCode)
            .orElseThrow(() -> new NotFoundException("vendingServiceRouteConfig " + ResponseStatus.NOT_FOUND.getMessage()));
        feeMapping.setFeeType(feeMappingRequest.getFeeType());
        feeMapping.setAmount(feeMappingRequest.getAmount());
        feeMapping.setCreatedAt(new Date());
        feeMapping.setUpdatedAt(new Date());
        feeMappingRepository.save(feeMapping);
        DefaultResponse defaultResponse = new DefaultResponse(
            ResponseStatus.SUCCESS.getMessage(),
            ResponseStatus.SUCCESS.getCode(),
            feeMapping
        );
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }
}
