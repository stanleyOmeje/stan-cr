package com.stan.stancore.extended.cron;

import com.systemspecs.remita.vending.extended.config.VendingCoreProperties;
import com.systemspecs.remita.vending.extended.dto.ekedcdto.request.VendorInformationRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.service.EkedcService;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KeepAliveSchedule {

    private final VendingCoreProperties properties;
    private final EkedcService ekedcService;

    @Scheduled(cron = "${vending.cron.keepalive.duration: * * * * * *}")
    public void keepEkoAlive() {
        ResponseEntity<DefaultResponse> response;
        if (properties.isEnableEkedcKeepAlive()) {
            log.info("Inside Keep alive schedule");
            VendorInformationRequest request = getVendorInformationRequest();
            log.info("Request inside keepEkoAlive: {}", request);
            response = ekedcService.vendorInfo(request);
            if (response.getBody() != null) {
                if (TransactionStatus.SUCCESS.getCode().equals(response.getBody().getStatus())) {
                    log.info("EKEDC service is up and running");
                } else {
                    log.info("EKEDC service is down");
                }
            } else {
                log.info("EKEDC service is down");
            }
        }
    }

    private VendorInformationRequest getVendorInformationRequest() {
        VendorInformationRequest request = new VendorInformationRequest();
        request.setIdVendor(properties.getIdVendor());
        request.setCodUser(properties.getCodUser());
        return request;
    }
}
