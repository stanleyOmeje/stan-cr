package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.dtonemodule.dto.response.MobileServiceProviderDto;
import com.systemspecs.remita.vending.dtonemodule.service.DtOneVendingWebService;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.MobileNumberLookUpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class MobileNumberLookUpServiceImpl implements MobileNumberLookUpService {

    private final DtOneVendingWebService dtOneVendingWebService;

    @Override
    public ResponseEntity<DefaultResponse> lookUpMobileNumber(String number) {
        log.info("<<<lookUpMobileNumber {}", dtOneVendingWebService.lookUpMobileNumber(number));
        List<MobileServiceProviderDto> providerDtos = dtOneVendingWebService.lookUpMobileNumber(number);
        if (providerDtos == null) {
            throw new NotFoundException("Providers not found");
        }
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setData(providerDtos);

        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }
}
