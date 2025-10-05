package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.enums.Processors;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.exception.UnknownException;
import com.systemspecs.remita.vending.extended.service.ProcessorPackageService;
import com.systemspecs.remita.vending.vendingcommon.dto.request.ProcessorPackageRequest;
import com.systemspecs.remita.vending.vendingcommon.entity.Product;
import com.systemspecs.remita.vending.vendingcommon.entity.VendingServiceRouteConfig;
import com.systemspecs.remita.vending.vendingcommon.factory.VendingServiceDelegateBean;
import com.systemspecs.remita.vending.vendingcommon.flight.factory.FlightProductVendingServiceDelegateBean;
import com.systemspecs.remita.vending.vendingcommon.flight.service.FlightProductAbstractVendingService;
import com.systemspecs.remita.vending.vendingcommon.repository.ProductRepository;
import com.systemspecs.remita.vending.vendingcommon.repository.VendingServiceRouteConfigRepository;
import com.systemspecs.remita.vending.vendingcommon.service.AbstractVendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.systemspecs.remita.vending.extended.enums.ResponseStatus.INTERNAL_SERVER_ERROR;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessorPackageServiceImp implements ProcessorPackageService {

    private final VendingServiceDelegateBean vendingServiceDelegateBean;

    private final ProductRepository productRepository;

    private final VendingServiceRouteConfigRepository routeConfigRepository;

    private final FlightProductVendingServiceDelegateBean flightProductVendingServiceDelegateBean;

    private static final String SYSTEM_PRODUCT_TYPE = "FLIGHT";

    @Override
    public ResponseEntity<DefaultResponse> createVendingPackage(ProcessorPackageRequest request, String processorId) {
        log.info(">>> Creating VendingProcessorPackage from processorId: {}", processorId);
        Object response = null;
        validateProcessorId(processorId);
        Product product = productRepository
            .findByCode(request.getProductCode())
            .orElseThrow(() -> new NotFoundException("product " + ResponseStatus.NOT_FOUND.getMessage()));

        //update for product
        Optional<VendingServiceRouteConfig> vendingServiceRouteConfig = routeConfigRepository.findFirstByIgnoreCaseProcessorId(processorId);
        if (vendingServiceRouteConfig.isEmpty()) {
            throw new NotFoundException(ResponseStatus.NOT_FOUND.getMessage(), ResponseStatus.NOT_FOUND.getCode());
        }
        String systemProductType = vendingServiceRouteConfig.get().getSystemProductType();
        if (SYSTEM_PRODUCT_TYPE.equalsIgnoreCase(systemProductType)) {
            FlightProductAbstractVendingService flightProductAbstractVendingService = getFlightProductVendingService(processorId);
            response = flightProductAbstractVendingService.createVendingProcessorPackage(request);
        } else {
            AbstractVendingService vendingService = getVendingService(processorId);
            response = vendingService.createVendingProcessorPackage(request);
        }

        if (response == null) {
            throw new NotFoundException(ResponseStatus.NOT_FOUND.getMessage(), ResponseStatus.NOT_FOUND.getCode());
        }
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setData(response);
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DefaultResponse> fetchProcessPackage(String processorId) {
        Object response = null;
        log.info(">>> Getting VendingProcessorPackage from processorId: {}", processorId);
        validateProcessorId(processorId);
        //update for product
        Optional<VendingServiceRouteConfig> vendingServiceRouteConfig = routeConfigRepository.findFirstByIgnoreCaseProcessorId(processorId);
        if (vendingServiceRouteConfig.isEmpty()) {
            throw new NotFoundException(ResponseStatus.NOT_FOUND.getMessage(), ResponseStatus.NOT_FOUND.getCode());
        }
        String systemProductType = vendingServiceRouteConfig.get().getSystemProductType();
        if (SYSTEM_PRODUCT_TYPE.equalsIgnoreCase(systemProductType)) {
            FlightProductAbstractVendingService flightProductAbstractVendingService = getFlightProductVendingService(processorId);
            response = flightProductAbstractVendingService.getVendingProcessorPackage();
        } else {
            AbstractVendingService vendingService = getVendingService(processorId);

            response = vendingService.getVendingProcessorPackage();
        }

        if (response == null) {
            throw new NotFoundException(ResponseStatus.NOT_FOUND.getMessage(), ResponseStatus.NOT_FOUND.getCode());
        }
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setData(response);
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    private static void validateProcessorId(String processor) {
        //log.info(">>> Validating ProcessorId: {}");
        boolean validProcessorId = Arrays.stream(Processors.values()).map(Enum::toString).collect(Collectors.toList()).contains(processor);
        if (!validProcessorId) {
            throw new NotFoundException("processor " + ResponseStatus.NOT_FOUND.getMessage());
        }
    }

    private AbstractVendingService getVendingService(String processorId) {
        log.info(">>> Getting VendingService from processorId: {}", processorId);
        AbstractVendingService serviceBean = vendingServiceDelegateBean.getDelegate(processorId);
        if (Objects.isNull(serviceBean)) {
            throw new UnknownException(ResponseStatus.INTERNAL_SERVER_ERROR.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR.getCode());
        }
        return serviceBean;
    }

    private FlightProductAbstractVendingService getFlightProductVendingService(String processorId) {
        log.info(">>> Getting flightProductVendingService from processorId):{}", processorId);
        FlightProductAbstractVendingService serviceBean = flightProductVendingServiceDelegateBean.getDelegate(processorId);
        if (Objects.isNull(serviceBean)) {
            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }
        return serviceBean;
    }
}
