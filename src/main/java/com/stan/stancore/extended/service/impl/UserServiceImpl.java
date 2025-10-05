package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.exception.UnknownException;
import com.systemspecs.remita.vending.extended.factory.AdminPortalFactory;
import com.systemspecs.remita.vending.extended.service.UserService;
import com.systemspecs.remita.vending.vendingcommon.dto.request.AdminUserRequest;
import com.systemspecs.remita.vending.vendingcommon.dto.response.AdminResponse;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import com.systemspecs.remita.vending.vendingcommon.factory.VendingServiceDelegateBean;
import com.systemspecs.remita.vending.vendingcommon.service.AbstractVendingService;
import com.systemspecs.remita.vending.vendingcommon.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static com.systemspecs.remita.vending.extended.enums.ResponseStatus.INTERNAL_SERVER_ERROR;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final VendingServiceDelegateBean vendingServiceDelegateBean;
    private final AdminPortalFactory adminPortalFactory;
    private static final String PROCESSOR_NOT_FOUND = "Cannot find processor";
    private static final String FETCHING_VENDING_SERVICE = ">>> Fetching the vendingService";
    private static final String NO_ACCOUNT_FOUND = "Cannot find account";
    private static final String REQUEST_NOT_FOUND = "Request not present";
    private static final String VENDOR_ID = "Vendor Id ...{}";

    @Override
    public ResponseEntity<AdminResponse> addUser(AdminUserRequest request) {
        log.info("Inside Add/Create User with AdminUserRequest");
        AdminResponse response = new AdminResponse();
        response.setCode(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());

        if (request == null) {
            response.setMessage(REQUEST_NOT_FOUND);
            response.setCode(TransactionStatus.TRANSACTION_FAILED.getCode());
            ResponseEntity.ok(response);
        }

        if (request.getVendorId() != 0) {
            log.info("Vendor ID: {}", request.getVendorId());
        }
        String processorId = request.getProcessorId();
        if (processorId == null) {
            response.setCode(ResponseStatus.NOT_FOUND.getCode());
            response.setMessage("Processor not found");
            return ResponseEntity.ok(response);
        }

        log.info(FETCHING_VENDING_SERVICE);
        AbstractVendingService vendingService = getVendingService(request.getProcessorId());
        UserManagementService concreteService = adminPortalFactory.getUserManagementService(processorId);
        if (Objects.isNull(vendingService) || Objects.isNull(concreteService)) {
            log.info(PROCESSOR_NOT_FOUND);
            return ResponseEntity.ok(response);
        }
        try {
            AdminResponse adminResponse = concreteService.createUser(request);
            if (adminResponse == null) {
                throw new NotFoundException(NO_ACCOUNT_FOUND);
            }
            if (!adminResponse.getCode().equalsIgnoreCase(ResponseStatus.SUCCESS.getCode())) {
                throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
            }
            return ResponseEntity.ok(adminResponse);
        } catch (Exception e) {
            log.error("Error creating user", e);
        }

        return null;
    }

    @Override
    public ResponseEntity<AdminResponse> updateUser(AdminUserRequest request) {
        log.info("Inside Modify User with AdminUserRequest");
        AdminResponse adminResponse = new AdminResponse();
        adminResponse.setCode(TransactionStatus.TRANSACTION_FAILED.getCode());
        adminResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());

        if (request == null) {
            log.error("Request is null");
            adminResponse.setMessage(REQUEST_NOT_FOUND);
            adminResponse.setCode(TransactionStatus.TRANSACTION_FAILED.getCode());
            ResponseEntity.ok(adminResponse);
        }

        if (request.getVendorId() != 0) {
            log.info("Vendor ID: {}", request.getVendorId());
        }
        String processorId = request.getProcessorId();
        if (processorId == null) {
            log.error("Processor ID is null");
            adminResponse.setCode(ResponseStatus.NOT_FOUND.getCode());
            adminResponse.setMessage(PROCESSOR_NOT_FOUND);
            return ResponseEntity.ok(adminResponse);
        }

        log.info(FETCHING_VENDING_SERVICE);
        AbstractVendingService vendingService = getVendingService(request.getProcessorId());
        UserManagementService concreteService = adminPortalFactory.getUserManagementService(processorId);
        if (Objects.isNull(vendingService) || Objects.isNull(concreteService)) {
            log.info(PROCESSOR_NOT_FOUND);
            return ResponseEntity.ok(adminResponse);
        }
        try {
            AdminResponse response = concreteService.modifyUser(request);
            if (response == null) {
                throw new NotFoundException(NO_ACCOUNT_FOUND);
            }
            if (!response.getCode().equalsIgnoreCase(ResponseStatus.SUCCESS.getCode())) {
                throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error modifying user", e);
        }

        return null;
    }

    @Override
    public ResponseEntity<AdminResponse> getUser(AdminUserRequest request) {
        log.info("Inside Search User with AdminUserRequest");
        AdminResponse response = new AdminResponse();
        response.setCode(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());

        if (request == null) {
            log.error("Request is null");
            response.setMessage(REQUEST_NOT_FOUND);
            response.setCode(TransactionStatus.TRANSACTION_FAILED.getCode());
            ResponseEntity.ok(response);
        }
        log.info(VENDOR_ID, request.getVendorId());

        String processorId = request.getProcessorId();
        if (processorId == null) {
            response.setCode(ResponseStatus.NOT_FOUND.getCode());
            response.setMessage(PROCESSOR_NOT_FOUND);
            return ResponseEntity.ok(response);
        }

        log.info(FETCHING_VENDING_SERVICE);
        AbstractVendingService vendingService = getVendingService(request.getProcessorId());
        UserManagementService concreteService = adminPortalFactory.getUserManagementService(processorId);
        if (Objects.isNull(vendingService) || Objects.isNull(concreteService)) {
            log.info(PROCESSOR_NOT_FOUND);
            return ResponseEntity.ok(response);
        }
        try {
            AdminResponse searchUser = concreteService.searchUser(request);
            if (searchUser == null) {
                throw new NotFoundException(NO_ACCOUNT_FOUND);
            }
            if (!searchUser.getCode().equalsIgnoreCase(ResponseStatus.SUCCESS.getCode())) {
                throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
            }
            return ResponseEntity.ok(searchUser);
        } catch (Exception e) {
            log.error("Error getting user", e);
        }

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
