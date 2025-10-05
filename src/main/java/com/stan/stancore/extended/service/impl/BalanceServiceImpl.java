package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.dtonemodule.dto.response.BalanceDto;
import com.systemspecs.remita.vending.dtonemodule.service.DtOneVendingWebService;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.BalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class BalanceServiceImpl implements BalanceService {

    private final DtOneVendingWebService vendingWebService;

    @Override
    public ResponseEntity<DefaultResponse> fetchBalance() {
        log.info("<<<Fetch Balance {}", vendingWebService.getBalances());
        List<BalanceDto> balances = vendingWebService.getBalances();
        if (balances == null) {
            throw new NotFoundException("Balances not Found");
        }
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setData(balances);

        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }
}
