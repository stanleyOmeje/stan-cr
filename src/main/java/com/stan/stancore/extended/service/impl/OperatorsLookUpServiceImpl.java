package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.dtonemodule.dto.request.OperatorsLookUpRequest;
import com.systemspecs.remita.vending.dtonemodule.dto.response.OperatorsLookUpResponse;
import com.systemspecs.remita.vending.dtonemodule.service.DtOneVendingWebService;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.OperatorsLookUpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OperatorsLookUpServiceImpl implements OperatorsLookUpService {

    private final DtOneVendingWebService webService;

    @Override
    public ResponseEntity<DefaultResponse> lookUpOperators(OperatorsLookUpRequest request) {
        List<OperatorsLookUpResponse> responseList = webService.lookupOperators(request);
        log.info("<<<lookUpOperators {}", responseList);
        if (responseList.isEmpty()) {
            throw new NotFoundException("Operators not found");
        }

        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setStatus("00");
        defaultResponse.setMessage("SUCCESS");
        defaultResponse.setData(responseList);

        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }
}
