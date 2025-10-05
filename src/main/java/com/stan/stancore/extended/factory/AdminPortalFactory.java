package com.stan.stancore.extended.factory;

import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.UnknownException;
import com.systemspecs.remita.vending.vendingcommon.factory.VendingServiceDelegateBean;
import com.systemspecs.remita.vending.vendingcommon.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;

import static com.systemspecs.remita.vending.extended.enums.ResponseStatus.INTERNAL_SERVER_ERROR;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminPortalFactory {

    private final VendingServiceDelegateBean vendingServiceDelegateBean;
    private static final String GETTING_VENDING_SERVICE = ">>> Getting VendingService from processorId):{}";

    public AuthPortalService getAuthPortalService(String processorId) {
        log.info(GETTING_VENDING_SERVICE, processorId);

        AbstractVendingService vendingService = getVendingService(processorId);
        if (!(vendingService instanceof AuthPortalService)) {
            throw new UnknownException(ResponseStatus.INTERNAL_SERVER_ERROR.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR.getCode());
        }
        AuthPortalService concreteService = vendingServiceDelegateBean.getAuthDelegate(processorId);
        if (Objects.isNull(concreteService)) {
            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }
        return concreteService;
    }

    public UserManagementService getUserManagementService(String processorId) {
        log.info("Getting UserManagementService for processorId: {}", processorId);

        AbstractVendingService vendingService = getVendingService(processorId);

        if (!(vendingService instanceof UserManagementService)) {
            throw new UnknownException(
                "Invalid vending service type for processorId: " + processorId,
                ResponseStatus.INTERNAL_SERVER_ERROR.getCode()
            );
        }

        UserManagementService concreteService = vendingServiceDelegateBean.getUserDelegate(processorId);

        if (concreteService == null) {
            throw new UnknownException(
                "Failed to retrieve concrete UserManagementService for processorId: " + processorId,
                ResponseStatus.INTERNAL_SERVER_ERROR.getCode()
            );
        }

        return concreteService;
    }

    public ProcessorTransactionService getProcessorTransaction(String processorId) {
        log.info("Getting ProcessorTransaction for processorId: {}", processorId);

        AbstractVendingService vendingService = getVendingService(processorId);

        if (!(vendingService instanceof UserManagementService)) {
            throw new UnknownException(
                "Invalid vending service type for processorId: " + processorId,
                ResponseStatus.INTERNAL_SERVER_ERROR.getCode()
            );
        }

        ProcessorTransactionService concreteService = vendingServiceDelegateBean.getProcessorTransactionDelegate(processorId);

        if (concreteService == null) {
            throw new UnknownException(
                "Failed to retrieve concrete UserManagementService for processorId: " + processorId,
                ResponseStatus.INTERNAL_SERVER_ERROR.getCode()
            );
        }

        return concreteService;
    }

    public ProcessorUniqueService getProcessorUniqueService(String processorId) {
        log.info(GETTING_VENDING_SERVICE, processorId);
        ProcessorUniqueService concreteService = null;
        try {
            concreteService = vendingServiceDelegateBean.getProcessorUniqueDelegate(processorId);
        } catch (Exception e) {
            log.info("Error getting auth portal, Module may not be a member of AuthPorter set...{}", e.getMessage());
        }
        return concreteService;
    }

    private AbstractVendingService getVendingService(String processorId) {
        log.info(GETTING_VENDING_SERVICE, processorId);
        AbstractVendingService serviceBean = vendingServiceDelegateBean.getDelegate(processorId);
        if (Objects.isNull(serviceBean)) {
            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }
        return serviceBean;
    }
}
