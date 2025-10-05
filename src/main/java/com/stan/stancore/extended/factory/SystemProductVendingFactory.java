package com.stan.stancore.extended.factory;

import com.systemspecs.remita.vending.extended.exception.UnknownException;
import com.systemspecs.remita.vending.vendingcommon.flight.factory.FlightProductVendingServiceDelegateBean;
import com.systemspecs.remita.vending.vendingcommon.flight.service.FlightProductAbstractVendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

import static com.systemspecs.remita.vending.extended.enums.ResponseStatus.INTERNAL_SERVER_ERROR;

@Slf4j
@RequiredArgsConstructor
public class SystemProductVendingFactory<T> {
    private final FlightProductVendingServiceDelegateBean flightProductVendingServiceDelegateBean;


    private FlightProductAbstractVendingService getFlightProductVendingService(String processorId) {
        log.info(">>> Getting flightProductVendingService from processorId):{}", processorId);
        FlightProductAbstractVendingService serviceBean = flightProductVendingServiceDelegateBean.getDelegate(processorId);
        if (Objects.isNull(serviceBean)) {
            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }
        return serviceBean;
    }
}
