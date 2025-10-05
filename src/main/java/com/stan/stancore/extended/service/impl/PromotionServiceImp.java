package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.dtonemodule.dto.response.PromotionResponse;
import com.systemspecs.remita.vending.dtonemodule.service.DtOneVendingWebService;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class PromotionServiceImp implements PromotionService {

    private final DtOneVendingWebService vendingWebService;

    @Override
    public ResponseEntity<DefaultResponse> fetchPromotion() {
        List<PromotionResponse> promotionResponses = vendingWebService.getPromotion();
        if (promotionResponses.isEmpty()) {
            throw new NotFoundException("Operator not found");
        }
        log.info("getOperators()  response {} ", promotionResponses);
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setData(promotionResponses);

        return ResponseEntity.ok(defaultResponse);
    }

    @Override
    public ResponseEntity<DefaultResponse> fetchPromotionById(String id) {
        PromotionResponse operatorResponses = vendingWebService.getPromotionById(id);
        if (Objects.isNull(operatorResponses)) {
            throw new NotFoundException("Operator not found");
        }
        log.info("getOperators()  response {} ", operatorResponses);
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setData(operatorResponses);

        return ResponseEntity.ok(defaultResponse);
    }
}
