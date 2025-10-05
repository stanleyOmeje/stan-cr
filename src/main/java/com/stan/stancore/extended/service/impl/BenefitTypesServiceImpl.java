package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.dtonemodule.dto.response.BenefitTypeDto;
import com.systemspecs.remita.vending.dtonemodule.service.DtOneVendingWebService;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.BenefitTypesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class BenefitTypesServiceImpl implements BenefitTypesService {

    private final DtOneVendingWebService vendingWebService;

    @Override
    public ResponseEntity<DefaultResponse> fetchBenefitTypes() {
        log.info("<<<Fetch BenefitTypes{}", vendingWebService.getBenefitTypes());
        List<BenefitTypeDto> benefitTypes = vendingWebService.getBenefitTypes();
        if (benefitTypes == null) {
            throw new NotFoundException("Benefit Types not Found");
        }
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setData(benefitTypes);

        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }
}
