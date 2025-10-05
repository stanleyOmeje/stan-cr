package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.exception.UnknownException;
import com.systemspecs.remita.vending.extended.factory.AdminPortalFactory;
import com.systemspecs.remita.vending.extended.service.ProcessorTransactionServiceByAdmin;
import com.systemspecs.remita.vending.vendingcommon.dto.request.AdminProcessorTransactionRequest;
import com.systemspecs.remita.vending.vendingcommon.dto.response.AdminResponse;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import com.systemspecs.remita.vending.vendingcommon.factory.VendingServiceDelegateBean;
import com.systemspecs.remita.vending.vendingcommon.service.AbstractVendingService;
import com.systemspecs.remita.vending.vendingcommon.service.ProcessorTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static com.systemspecs.remita.vending.extended.enums.ResponseStatus.INTERNAL_SERVER_ERROR;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessorTransactionServiceByAdminImpl implements ProcessorTransactionServiceByAdmin {

    private final VendingServiceDelegateBean vendingServiceDelegateBean;
    private final AdminPortalFactory adminPortalFactory;
    private static final String PROCESSOR_NOT_FOUND = "Cannot find processor";
    private static final String FETCHING_VENDING_SERVICE = ">>> Fetching the vendingService";
    private static final String NO_ACCOUNT_FOUND = "Cannot find account";
    private static final String REQUEST_NOT_FOUND = "Request not present";
    private static final String VENDOR_ID = "Vendor Id ...{}";

    @Override
    public ResponseEntity<AdminResponse> getAllProcessorTransaction(AdminProcessorTransactionRequest request) {
        log.info("Inside fetch all transaction");

        AdminResponse response = new AdminResponse();
        response.setCode(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());

        if (request == null) {
            response.setMessage(REQUEST_NOT_FOUND);
            response.setCode(TransactionStatus.TRANSACTION_FAILED.getCode());
        }

        if (request.getVendorId() != 0) {
            log.info(VENDOR_ID, request.getVendorId());
        }
        String processorId = request.getProcessorId();
        if (processorId == null) {
            response.setCode(ResponseStatus.NOT_FOUND.getCode());
            response.setMessage(PROCESSOR_NOT_FOUND);
        }

        log.info(FETCHING_VENDING_SERVICE);
        AbstractVendingService vendingService = getVendingService(request.getProcessorId());
        ProcessorTransactionService concreteService = adminPortalFactory.getProcessorTransaction(processorId);
        if (Objects.isNull(vendingService) || Objects.isNull(concreteService)) {
            log.info(PROCESSOR_NOT_FOUND);
            return ResponseEntity.ok(response);
        }
        try {
            AdminResponse adminResponse = concreteService.fetchAllTransaction(request);
            if (adminResponse == null) {
                throw new NotFoundException(NO_ACCOUNT_FOUND);
            }
            if (!adminResponse.getCode().equalsIgnoreCase(ResponseStatus.SUCCESS.getCode())) {
                throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
            }
            return ResponseEntity.ok(adminResponse);
        } catch (Exception e) {
            log.error("Error fetching transaction for processor", e);
        }

        return null;
    }

    @Override
    public ResponseEntity<AdminResponse> getTransactionByReference(AdminProcessorTransactionRequest request) {
        return null;
    }

    private AbstractVendingService getVendingService(String processorId) {
        log.info(">>> Getting VendingService from processorId):{}", processorId);
        AbstractVendingService serviceBean = vendingServiceDelegateBean.getDelegate(processorId);
        if (Objects.isNull(serviceBean)) {
            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }
        return serviceBean;
    }
}
