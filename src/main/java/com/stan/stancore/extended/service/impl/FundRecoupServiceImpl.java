package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.extended.dto.request.BulkDebitListRequest;
import com.systemspecs.remita.vending.extended.dto.request.FundRecoupRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.dto.response.FundRecoupErrorMap;
import com.systemspecs.remita.vending.extended.dto.response.FundRecoupErrorResponse;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.AlreadyExistException;
import com.systemspecs.remita.vending.extended.service.FundRecoupService;
import com.systemspecs.remita.vending.vendingcommon.entity.FundRecoup;
import com.systemspecs.remita.vending.vendingcommon.enums.Action;
import com.systemspecs.remita.vending.vendingcommon.enums.DebitStatus;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import com.systemspecs.remita.vending.vendingcommon.repository.FundRecoupRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
@Service
public class FundRecoupServiceImpl implements FundRecoupService {

    private final FundRecoupRepository fundRecoupRepository;
    private static final String FUNDRECOUP = "Fund recoup transaction already exists";

    @Override
    public ResponseEntity<DefaultResponse> createFundRecoup(FundRecoupRequest request) {
        log.info(">>> Creating DebitList with request: {}", request);
        String ref = request.getReference();
        Optional<FundRecoup> debitCheck = fundRecoupRepository.findByReference(ref);
        if (debitCheck.isPresent()) {
            throw new AlreadyExistException(FUNDRECOUP + " " + ResponseStatus.ALREADY_EXIST.getMessage());
        }

        FundRecoup fundRecoup = new FundRecoup();
        fundRecoup.setReference(ref);
        fundRecoup.setStatus(DebitStatus.OPEN);
        fundRecoup.setAction(Action.DEBIT);
        fundRecoup.setCreatedAt(new Date());

        fundRecoupRepository.save(fundRecoup);
        DefaultResponse defaultResponse = new DefaultResponse(
            ResponseStatus.SUCCESS.getMessage(),
            ResponseStatus.SUCCESS.getCode(),
            fundRecoup
        );
        return new ResponseEntity<>(defaultResponse, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<DefaultResponse> createBulkFundRecoup(BulkDebitListRequest bulkDebitListRequest) {
        log.info(">>> Creating BulkDebitList with request: {}", bulkDebitListRequest);
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        List<FundRecoup> fundRecoups = new ArrayList<>();
        FundRecoupErrorResponse errorResponse = new FundRecoupErrorResponse();
        try {
            List<String> clientReferenceList = bulkDebitListRequest
                .getItems()
                .stream()
                .map(item -> {
                    String clientReference = item.getReference();
                    return clientReference;
                })
                .collect(Collectors.toList());
            log.info("clientReference list inside createBulkDebitList for loop: {}", clientReferenceList);
            clientReferenceList.forEach(reference -> {
                Optional<FundRecoup> debitCheck = fundRecoupRepository.findByReference(reference);
                if (debitCheck.isPresent()) {
                    FundRecoupErrorMap errorMap = new FundRecoupErrorMap();
                    errorMap.setReference(reference);
                    errorMap.setErrorMessage(FUNDRECOUP);
                    errorResponse.getErrorResponse().add(errorMap);
                }
                fundRecoups.add(mapReferenceToDebitList(reference));
            });
            if (!errorResponse.getErrorResponse().isEmpty()) {
                defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
                defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
                defaultResponse.setData(errorResponse.getErrorResponse());
                return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
            }
            fundRecoupRepository.saveAll(fundRecoups);
            defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
            defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
            defaultResponse.setData(fundRecoups);
            log.info("defaultResponse data inside createBulkDebitList is: {}", defaultResponse);
            return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
        } catch (AlreadyExistException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    private FundRecoup mapReferenceToDebitList(String reference) {
        FundRecoup dList = new FundRecoup();
        dList.setReference(reference);
        dList.setStatus(DebitStatus.OPEN);
        dList.setCreatedAt(new Date());
        return dList;
    }
}
