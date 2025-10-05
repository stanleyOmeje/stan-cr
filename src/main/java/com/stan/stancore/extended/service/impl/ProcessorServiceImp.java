package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.enums.Processors;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.exception.UnknownException;
import com.systemspecs.remita.vending.extended.service.ProcessorService;
import com.systemspecs.remita.vending.vendingcommon.dto.response.GetBalanceResponse;
import com.systemspecs.remita.vending.vendingcommon.dto.response.GetServiceResponse;
import com.systemspecs.remita.vending.vendingcommon.entity.Transaction;
import com.systemspecs.remita.vending.vendingcommon.factory.VendingServiceDelegateBean;
import com.systemspecs.remita.vending.vendingcommon.repository.TransactionRepository;
import com.systemspecs.remita.vending.vendingcommon.service.AbstractVendingService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcessorServiceImp implements ProcessorService {

    private final VendingServiceDelegateBean vendingServiceDelegateBean;

    private final TransactionRepository transactionRepository;

    Logger log = LoggerFactory.getLogger(ProcessorServiceImp.class);

    @Override
    public ResponseEntity<DefaultResponse> getWalletBalance(String processorId) {
        log.info(">>> Getting WalletBalance from processorId: {}", processorId);
        validateProcessorId(processorId);
        AbstractVendingService vendingService = getVendingService(processorId);

        GetBalanceResponse response = vendingService.getBalance();
        if (response == null) {
            throw new NotFoundException(ResponseStatus.NOT_FOUND.getMessage(), ResponseStatus.NOT_FOUND.getCode());
        }
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setData(response);
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    private AbstractVendingService getVendingService(String processorId) {
        log.info(">>> Getting VendingService from processorId: {}", processorId);
        AbstractVendingService serviceBean = vendingServiceDelegateBean.getDelegate(processorId);
        if (Objects.isNull(serviceBean)) {
            throw new UnknownException(ResponseStatus.INTERNAL_SERVER_ERROR.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR.getCode());
        }
        return serviceBean;
    }

    private static void validateProcessorId(String processor) {
        //log.info(">>> Validating ProcessorId: {}");
        boolean validProcessorId = Arrays.stream(Processors.values()).map(Enum::toString).collect(Collectors.toList()).contains(processor);
        if (!validProcessorId) {
            throw new NotFoundException("processor " + ResponseStatus.NOT_FOUND.getMessage());
        }
    }

    @Override
    public ResponseEntity<DefaultResponse> getAllProcessors() {
        log.info(">>> Getting All the Processors");
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setData(Processors.values());
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DefaultResponse> getProcessorByReference(String ref) {
        Map<String, String> responseMap = new HashMap<>();
        Optional<Transaction> transaction = transactionRepository.findByClientReference(ref);
        if (transaction.isEmpty()) {
            throw new NotFoundException("Transaction with reference " + ref + " not present");
        }
        log.info(">>> Getting the Processor with ref ...{}", ref);
        responseMap.put("processorId", transaction.get().getProcessorId().toUpperCase());
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setData(responseMap);
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DefaultResponse> getServices(String processorId) {
        log.info(">>> Getting Services from processorId: {}", processorId);
        validateProcessorId(processorId);
        AbstractVendingService vendingService = getVendingService(processorId);

        GetServiceResponse response = vendingService.getServices();
        if (response == null) {
            throw new NotFoundException(ResponseStatus.NOT_FOUND.getMessage(), ResponseStatus.NOT_FOUND.getCode());
        }
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setData(response);
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }
}
