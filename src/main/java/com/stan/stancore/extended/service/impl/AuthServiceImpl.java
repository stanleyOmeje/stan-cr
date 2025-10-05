package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.exception.UnknownException;
import com.systemspecs.remita.vending.extended.factory.AdminPortalFactory;
import com.systemspecs.remita.vending.extended.service.AuthService;
import com.systemspecs.remita.vending.vendingcommon.dto.request.AdminAuthRequest;
import com.systemspecs.remita.vending.vendingcommon.dto.response.AdminResponse;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import com.systemspecs.remita.vending.vendingcommon.factory.VendingServiceDelegateBean;
import com.systemspecs.remita.vending.vendingcommon.service.AbstractVendingService;
import com.systemspecs.remita.vending.vendingcommon.service.AuthPortalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static com.systemspecs.remita.vending.extended.enums.ResponseStatus.INTERNAL_SERVER_ERROR;

@RequiredArgsConstructor
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final VendingServiceDelegateBean vendingServiceDelegateBean;
    private static final String PROCESSOR_NOT_FOUND = "Cannot find processor";
    private static final String ADMIN_REQUEST_ACCOUNT = ">>> AdminRequest account: {}";
    private static final String GETTING_VENDING_SERVICE = ">>> Getting the vendingService";
    private static final String ACCOUNT_NOT_FOUND = "Cannot find account";
    private static final String SERVICE_NOT_FOUND = "Cannot find service";
    private static final String REQUEST_EMPTY = "Request is empty";
    private static final String VENDOR_ID = "The vendor Id is ...{}";

    private final AdminPortalFactory adminPortalFactory;

    @Override
    public ResponseEntity<AdminResponse> performLogin(AdminAuthRequest request) {
        AdminResponse response = new AdminResponse();
        response.setCode(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        log.info(ADMIN_REQUEST_ACCOUNT, request);
        if (request == null) {
            response.setCode(TransactionStatus.TRANSACTION_FAILED.getCode());
            response.setMessage(REQUEST_EMPTY);
            return ResponseEntity.ok(response);
        }
        log.info(VENDOR_ID, request.getVendorId());
        String processorId = request.getProcessorId();
        if (processorId == null) {
            response.setCode(ResponseStatus.NOT_FOUND.getCode());
            response.setMessage(PROCESSOR_NOT_FOUND);
            return ResponseEntity.ok(response);
        }

        log.info(GETTING_VENDING_SERVICE);
        AbstractVendingService vendingServices = getVendingService(request.getProcessorId());

        AuthPortalService concreteService = adminPortalFactory.getAuthPortalService(processorId);
        if (Objects.isNull(vendingServices) || Objects.isNull(concreteService)) {
            log.info(PROCESSOR_NOT_FOUND);
            return ResponseEntity.ok(response);
        }
        try {
            AdminResponse result = concreteService.getLogin(request);
            if (result == null) {
                throw new NotFoundException(ACCOUNT_NOT_FOUND);
            }
            if (!result.getCode().equalsIgnoreCase(ResponseStatus.SUCCESS.getCode())) {
                throw new UnknownException(
                    ResponseStatus.INTERNAL_SERVER_ERROR.getMessage(),
                    ResponseStatus.INTERNAL_SERVER_ERROR.getCode()
                );
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ResponseEntity<AdminResponse> verifyPassword(AdminAuthRequest request) {
        AdminResponse response = new AdminResponse();
        response.setCode(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        log.info(ADMIN_REQUEST_ACCOUNT, request);
        if (request == null) {
            response.setCode(TransactionStatus.TRANSACTION_FAILED.getCode());
            response.setMessage(REQUEST_EMPTY);
            return ResponseEntity.ok(response);
        }
        log.info(VENDOR_ID, request.getVendorId());
        String processorId = request.getProcessorId();
        if (processorId == null) {
            response.setCode(ResponseStatus.NOT_FOUND.getCode());
            response.setMessage(PROCESSOR_NOT_FOUND);
            return ResponseEntity.ok(response);
        }

        log.info(GETTING_VENDING_SERVICE);
        AbstractVendingService vendingServices = getVendingService(request.getProcessorId());

        AuthPortalService concreteService = adminPortalFactory.getAuthPortalService(processorId);
        if (Objects.isNull(vendingServices) || Objects.isNull(concreteService)) {
            log.info(PROCESSOR_NOT_FOUND);
            return ResponseEntity.ok(response);
        }
        try {
            AdminResponse result = concreteService.validatePassword(request);
            if (result == null) {
                throw new NotFoundException(ACCOUNT_NOT_FOUND);
            }
            if (!result.getCode().equalsIgnoreCase(ResponseStatus.SUCCESS.getCode())) {
                throw new UnknownException(
                    ResponseStatus.INTERNAL_SERVER_ERROR.getMessage(),
                    ResponseStatus.INTERNAL_SERVER_ERROR.getCode()
                );
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ResponseEntity<AdminResponse> modifyPassword(AdminAuthRequest request) {
        AdminResponse response = new AdminResponse();
        response.setCode(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        log.info(ADMIN_REQUEST_ACCOUNT, request);
        if (request == null) {
            response.setCode(TransactionStatus.TRANSACTION_FAILED.getCode());
            response.setMessage(REQUEST_EMPTY);
            return ResponseEntity.ok(response);
        }
        log.info(VENDOR_ID, request.getVendorId());
        String processorId = request.getProcessorId();
        if (processorId == null) {
            response.setCode(ResponseStatus.NOT_FOUND.getCode());
            response.setMessage(PROCESSOR_NOT_FOUND);
            return ResponseEntity.ok(response);
        }

        log.info(GETTING_VENDING_SERVICE);
        AbstractVendingService vendingServices = getVendingService(request.getProcessorId());

        AuthPortalService concreteService = adminPortalFactory.getAuthPortalService(processorId);
        if (Objects.isNull(vendingServices) || Objects.isNull(concreteService)) {
            log.info(PROCESSOR_NOT_FOUND);
            return ResponseEntity.ok(response);
        }
        try {
            AdminResponse result = concreteService.changePassword(request);
            if (result == null) {
                throw new NotFoundException(ACCOUNT_NOT_FOUND);
            }
            if (!result.getCode().equalsIgnoreCase(ResponseStatus.SUCCESS.getCode())) {
                throw new UnknownException(
                    ResponseStatus.INTERNAL_SERVER_ERROR.getMessage(),
                    ResponseStatus.INTERNAL_SERVER_ERROR.getCode()
                );
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ResponseEntity<AdminResponse> forgotPassword(AdminAuthRequest request) {
        AdminResponse response = new AdminResponse();
        response.setCode(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        log.info(ADMIN_REQUEST_ACCOUNT, request);
        if (request == null) {
            response.setCode(TransactionStatus.TRANSACTION_FAILED.getCode());
            response.setMessage(REQUEST_EMPTY);
            return ResponseEntity.ok(response);
        }
        log.info(VENDOR_ID, request.getVendorId());
        String processorId = request.getProcessorId();
        if (processorId == null) {
            response.setCode(ResponseStatus.NOT_FOUND.getCode());
            response.setMessage(PROCESSOR_NOT_FOUND);
            return ResponseEntity.ok(response);
        }

        log.info(GETTING_VENDING_SERVICE);
        AbstractVendingService vendingServices = getVendingService(request.getProcessorId());

        AuthPortalService concreteService = adminPortalFactory.getAuthPortalService(processorId);
        if (Objects.isNull(vendingServices) || Objects.isNull(concreteService)) {
            log.info(PROCESSOR_NOT_FOUND);
            return ResponseEntity.ok(response);
        }
        try {
            AdminResponse result = concreteService.forgotPassword(request);
            if (result == null) {
                throw new NotFoundException(ACCOUNT_NOT_FOUND);
            }
            if (!result.getCode().equalsIgnoreCase(ResponseStatus.SUCCESS.getCode())) {
                throw new UnknownException(
                    ResponseStatus.INTERNAL_SERVER_ERROR.getMessage(),
                    ResponseStatus.INTERNAL_SERVER_ERROR.getCode()
                );
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private AbstractVendingService getVendingService(String processorId) {
        log.info(">>> Getting VendingService from pprocessorId):{}", processorId);
        AbstractVendingService serviceBean = vendingServiceDelegateBean.getDelegate(processorId);
        if (Objects.isNull(serviceBean)) {
            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }
        return serviceBean;
    }
}
