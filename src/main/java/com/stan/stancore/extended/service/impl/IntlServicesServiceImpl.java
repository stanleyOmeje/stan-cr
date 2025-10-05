package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.dtonemodule.dto.response.ServiceResponse;
import com.systemspecs.remita.vending.dtonemodule.service.DtOneVendingWebService;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.IntlServicesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class IntlServicesServiceImpl implements IntlServicesService {

    private final DtOneVendingWebService webService;

    public ResponseEntity<DefaultResponse> fetchServices() {
        log.info("<<<fetch Services {}", webService.getServices());
        List<ServiceResponse> services = webService.getServices();
        if (services.isEmpty()) {
            throw new NotFoundException("Services not found");
        }

        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setData(services);

        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DefaultResponse> fetchServiceById(Long id) {
        log.info("<<<fetch Service by code {}", id);
        ServiceResponse serviceResponse = webService.getServiceById(id);
        if (serviceResponse == null) {
            throw new NotFoundException("Service with id not found");
        }
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setData(serviceResponse);
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }
}
