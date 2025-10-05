package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.extended.dto.request.ValidationRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.BadRequestException;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.exception.UnknownException;
import com.systemspecs.remita.vending.extended.service.ValidationService;
import com.systemspecs.remita.vending.vendingcommon.dto.response.VerifyAccountResponse;
import com.systemspecs.remita.vending.vendingcommon.entity.Product;
import com.systemspecs.remita.vending.vendingcommon.enums.VerificationStatus;
import com.systemspecs.remita.vending.vendingcommon.factory.VendingServiceDelegateBean;
import com.systemspecs.remita.vending.vendingcommon.repository.ProductRepository;
import com.systemspecs.remita.vending.vendingcommon.service.AbstractVendingService;
import com.systemspecs.remita.vending.vendingcommon.service.VendingServiceProcessorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Objects;

import static com.systemspecs.remita.vending.extended.enums.ResponseStatus.INTERNAL_SERVER_ERROR;

@Service
public class ValidationServiceImp implements ValidationService {

    Logger log = LoggerFactory.getLogger(ValidationServiceImp.class);

    private final ProductRepository productRepository;
    private final VendingServiceDelegateBean vendingServiceDelegateBean;
    private final VendingServiceProcessorService vendingServiceProcessorService;

    public ValidationServiceImp(
        ProductRepository productRepository,
        VendingServiceDelegateBean vendingServiceDelegateBean,
        VendingServiceProcessorService vendingServiceProcessorService
    ) {
        this.productRepository = productRepository;
        this.vendingServiceDelegateBean = vendingServiceDelegateBean;
        this.vendingServiceProcessorService = vendingServiceProcessorService;
    }

    public ResponseEntity<DefaultResponse> validateAccount(ValidationRequest request) {
        log.info(">>> Validating account: {}", request);
        Product product = productRepository
            .findByCode(request.getProductCode())
            .orElseThrow(() -> new NotFoundException("product " + ResponseStatus.NOT_FOUND.getMessage()));
        log.info("The product is ...{}", product);
        String processorId = vendingServiceProcessorService.getProcessorId(product.getCode());
        if (processorId == null) {
            log.info("Cannot find processor");
            throw new UnknownException(ResponseStatus.INTERNAL_SERVER_ERROR.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR.getCode());
        }

        log.info(">>> Getting the vendingService");
        AbstractVendingService vendingService = getVendingService(vendingServiceDelegateBean, processorId);

        VerifyAccountResponse result = vendingService.verifyAccount(request.getUniqueIdentifier(), request.getProductCode());

        if (result == null) {
            throw new NotFoundException("Account not found");
        }
        if (result.getStatus() == VerificationStatus.SYSTEM) {
            log.info(result.getMessage());
            throw new BadRequestException(
                ResponseStatus.INTERNAL_SERVER_ERROR.getMessage(),
                ResponseStatus.INTERNAL_SERVER_ERROR.getCode()
            );
        }

        String fallbackProcessor = null;
        VerifyAccountResponse fallbackResult;
        String fallbackFailureMessage = null;
        String mainFailureMessage = null;
        boolean processedWithFallback = false;
        String fallbackResponseCode = null;

        if (result.getStatus() == VerificationStatus.FAILED) {
            //call fallback
            fallbackProcessor = getValidateFallbackProcessorId(request);

            log.info(" fallbackProcessor is ...{}", fallbackProcessor);
            if (fallbackProcessor != null) {
                mainFailureMessage = result == null ? "unknown error" : result.getMessage();
                vendingService = getVendingService(fallbackProcessor);
                fallbackResult = vendingService.verifyAccount(request.getUniqueIdentifier(), request.getProductCode());

                if (fallbackResult != null && fallbackResult.getStatus() != VerificationStatus.SUCCESSFUL) {
                    fallbackFailureMessage = fallbackResult.getMessage();
                    fallbackResponseCode = fallbackResult.getStatus().toString();
                }

                if (fallbackResult != null && fallbackResult.getStatus() == VerificationStatus.SUCCESSFUL) {
                    processedWithFallback = true;
                    result = fallbackResult;
                }
                log.info(
                    "fallbackFailureMessage...{},..mainFailureMessage...{}...processedWithFallback...{}...fallbackResponseCode...{}",
                    fallbackFailureMessage,
                    mainFailureMessage,
                    processedWithFallback,
                    fallbackResponseCode
                );
            }
        }

        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(result.getMessage());
        if (result.getStatus().equals(VerificationStatus.SUCCESSFUL)) {
            defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        } else {
            defaultResponse.setStatus(ResponseStatus.NOT_FOUND.getCode());
        }
        defaultResponse.setData(result.getData());
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    private String getValidateFallbackProcessorId(ValidationRequest request) {
        return vendingServiceProcessorService.getFallbackProcessorId(request.getProductCode());
    }

    private AbstractVendingService getVendingService(VendingServiceDelegateBean vendingServiceDelegateBean, String processorId) {
        AbstractVendingService serviceBean = vendingServiceDelegateBean.getDelegate(processorId);
        if (Objects.isNull(serviceBean)) {
            log.info(" unable to retrieve service bean : identity type ");
            throw new UnknownException(ResponseStatus.INTERNAL_SERVER_ERROR.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR.getCode());
        }
        return serviceBean;
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
