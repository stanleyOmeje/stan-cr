package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.dto.response.RevendErrorMap;
import com.systemspecs.remita.vending.extended.dto.response.RevendValidationResponse;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.BadRequestException;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.BulkRevendService;
import com.systemspecs.remita.vending.extended.util.ReferenceUtil;
import com.systemspecs.remita.vending.vendingcommon.dto.request.BulkRevendRequest;
import com.systemspecs.remita.vending.vendingcommon.entity.RevendItems;
import com.systemspecs.remita.vending.vendingcommon.entity.Transaction;
import com.systemspecs.remita.vending.vendingcommon.entity.TransactionData;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import com.systemspecs.remita.vending.vendingcommon.repository.RevendItemsRepository;
import com.systemspecs.remita.vending.vendingcommon.repository.TransactionDataRepository;
import com.systemspecs.remita.vending.vendingcommon.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkRevendServiceImpl implements BulkRevendService {

    private final RevendItemsRepository revendItemsRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionDataRepository transactionDataRepository;
    private static final String FILE = "File ";

    @Override
    public ResponseEntity<DefaultResponse> processBulkRevend(BulkRevendRequest bulkRevendRequest) {
        DefaultResponse response = new DefaultResponse();
        try {
            List<String> clientReferenceList = bulkRevendRequest
                .getItems()
                .stream()
                .map(item -> {
                    String clientReference = item.getPaymentIdentifier();
                    return clientReference;
                })
                .collect(Collectors.toList());
            log.info("clientReference list inside processBulkRevendFile for loop: {}", clientReferenceList);
            RevendValidationResponse revendValidationResponse = validateRevendItems(clientReferenceList);
            log.info("RevendValidationResponse inside processBulkRevendFile for loop: {}", revendValidationResponse);
            if (revendValidationResponse.getErrorResponse().size() > 0) {
                response.setStatus(ResponseStatus.FAILED_REQUIREMENT.getCode());
                response.setMessage(ResponseStatus.FAILED_REQUIREMENT.getMessage());
                response.setData(revendValidationResponse.getErrorResponse());
                return new ResponseEntity<>(response, HttpStatus.OK);
            }

            String bulkRevendReferenceId = ReferenceUtil.generateInternalReference();
            List<RevendItems> revendItems = mapRevendingItemRequestToRevendingItems(
                revendValidationResponse.getTransactionList(),
                bulkRevendReferenceId
            );
            revendItemsRepository.saveAll(revendItems);
            response.setStatus(ResponseStatus.SUCCESS.getCode());
            response.setMessage("BulkRevend Request successfully collected. Queued for processing");
            HashMap<String, Object> mapData = new HashMap<>();
            mapData.put("BulkRevendReferenceId", bulkRevendReferenceId);
            response.setData(mapData);
        } catch (BadRequestException e) {
            response.setStatus(e.getCode());
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Error saving bulk revend request {} ", e.getMessage());
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR.getCode());
            response.setMessage(ResponseStatus.INTERNAL_SERVER_ERROR.getMessage());
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DefaultResponse> processBulkRevendFile(MultipartFile file) {
        DefaultResponse response = new DefaultResponse();
        response.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        response.setMessage("Error Uploading File");
        List<RevendItems> revendItemsList = new ArrayList<>();
        String bulkRevendReferenceId = ReferenceUtil.generateInternalReference();
        if (file.isEmpty()) {
            throw new NotFoundException("File is empty");
        }
        boolean isCsvFile = isCSVFile(file);
        if (!isCsvFile) {
            throw new BadRequestException(
                "Incorrect File Format. Please provide a valid CSV file format ",
                ResponseStatus.BAD_REQUEST.getCode()
            );
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            List<String> clientReferenceList = new ArrayList<>();
            CSVParser csvParser = CSVFormat.DEFAULT.parse(reader);
            List<CSVRecord> records = csvParser.getRecords();
            records.forEach(strings -> System.out.println("Size of value " + strings.size()));
            for (int i = 1; i < records.size(); i++) {
                String clientReference = records.get(i).get(0);
                if (StringUtils.isBlank(clientReference)) {
                    continue;
                }
                clientReferenceList.add(clientReference);
            }
            log.info("clientReference list inside processBulkRevendFile for loop: {}", clientReferenceList);

            long clientReferenceListSize = clientReferenceList.size();
            log.info("clientReference list Size inside processBulkRevendFile: {}", clientReferenceListSize);
            if (clientReferenceList.isEmpty()) {
                throw new BadRequestException("Uploaded file can not be empty ", ResponseStatus.BAD_REQUEST.getCode());
            }

            RevendValidationResponse revendValidationResponse = validateRevendItems(clientReferenceList);
            log.info("RevendValidationResponse inside processBulkRevendFile for loop: {}", revendValidationResponse);
            if (revendValidationResponse.getErrorResponse().size() > 0) {
                response.setStatus(ResponseStatus.FAILED_REQUIREMENT.getCode());
                response.setMessage(ResponseStatus.FAILED_REQUIREMENT.getMessage());
                response.setData(revendValidationResponse.getErrorResponse());
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
            revendValidationResponse
                .getTransactionList()
                .forEach(transaction -> {
                    log.info("Transaction in  revendValidationResponse ...{}", transaction);
                    Optional<TransactionData> optionalTransactionData = transactionDataRepository.findByTransaction(transaction);
                    TransactionData transactionData = optionalTransactionData.get();
                    log.info("TransactionData in mapRevendingItemRequestToRevendingItems ...{}", transactionData);
                    RevendItems revendItems = new RevendItems();
                    revendItems.setPaymentIdentifier(transaction.getClientReference());
                    revendItems.setProductCode(transaction.getProductCode());
                    revendItems.setAccountNumber(transactionData.getAccountNumber());
                    revendItems.setVendStatus(TransactionStatus.PENDING);
                    revendItems.setBulkRevendReferenceId(bulkRevendReferenceId);
                    revendItems.setPhoneNumber(transactionData.getPhoneNumber());
                    revendItems.setCreatedAt(new Date());
                    revendItemsList.add(revendItems);
                });
            revendItemsRepository.saveAll(revendItemsList);
            response.setStatus(ResponseStatus.SUCCESS.getCode());
            response.setMessage("File Uploaded Successfully");
            HashMap<String, Object> map = new HashMap<>();
            map.put("BulkRevendReferenceId", bulkRevendReferenceId);
            response.setData(map);
        } catch (IOException e) {
            log.info("Failed to upload and process the file: " + e.getMessage());
            response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR.getCode());
            response.setMessage(ResponseStatus.INTERNAL_SERVER_ERROR.getMessage());
        } catch (BadRequestException e) {
            log.info("Failed to upload and process the file: " + e.getMessage());
            response.setStatus(e.getCode());
            response.setMessage(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private List<RevendItems> mapRevendingItemRequestToRevendingItems(List<Transaction> items, String bulkRevendReferenceId) {
        List<RevendItems> revendItemsList = new ArrayList<>();

        for (Transaction item : items) {
            String clientReference = item.getClientReference();
            Optional<Transaction> optionalTransaction = transactionRepository.findByClientReference(clientReference);
            if (optionalTransaction.isEmpty()) {
                throw new BadRequestException(
                    "Transaction with reference " + clientReference + " does not exist",
                    ResponseStatus.BAD_REQUEST.getCode()
                );
            }
            Transaction transaction = optionalTransaction.get();
            log.info("Transaction in  mapRevendingItemRequestToRevendingItems ...{}", transaction);
            Optional<TransactionData> optionalTransactionData = transactionDataRepository.findByTransaction(transaction);
            TransactionData transactionData = optionalTransactionData.get();
            log.info("Transaction data in mapRevendingItemRequestToRevendingItems ...{}", transactionData);
            RevendItems revendItems = RevendItems
                .builder()
                .productCode(transaction.getProductCode())
                .accountNumber(transactionData.getAccountNumber())
                .vendStatus(TransactionStatus.PENDING)
                .bulkRevendReferenceId(bulkRevendReferenceId)
                .phoneNumber(transactionData.getPhoneNumber())
                .paymentIdentifier(clientReference)
                .createdAt(new Date())
                .build();

            revendItemsList.add(revendItems);
        }
        return revendItemsList;
    }

    @Override
    public ResponseEntity<DefaultResponse> getUploadedFile(String bulkRevendReferenceId, Pageable pageable) {
        log.info("Inside getUploadedFile with bulkRevendReferenceId: {}", bulkRevendReferenceId);
        DefaultResponse defaultResponse = new DefaultResponse();

        Page<RevendItems> uploadedFile = revendItemsRepository.findAllByBulkRevendReferenceId(bulkRevendReferenceId, pageable);
        log.info("uploadedFile: ... {}", uploadedFile);
        if (Objects.isNull(uploadedFile)) {
            throw new NotFoundException(FILE + ResponseStatus.NOT_FOUND.getMessage());
        }

        Map<String, Object> map = new HashMap<>();
        map.put("totalPage", uploadedFile.getTotalPages());
        map.put("totalContent", uploadedFile.getTotalElements());
        map.put("items", uploadedFile.getContent());
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setData(map);

        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    private RevendValidationResponse validateRevendItems(List<String> clientReference) {
        log.info("Inside validateRevendItems with clientReference: {}", clientReference);
        RevendValidationResponse revendValidationResponse = new RevendValidationResponse();
        List<Transaction> transactionList = new ArrayList<>();
        for (String clientRef : clientReference) {
            Optional<Transaction> optionalTransaction = transactionRepository.findByClientReference(clientRef);
            if (optionalTransaction.isEmpty()) {
                RevendErrorMap revendErrorMap = new RevendErrorMap();
                revendErrorMap.setClientReference(clientRef);
                revendErrorMap.setErrorMessage("Transaction does not exist");
                revendValidationResponse.getErrorResponse().add(revendErrorMap);
            } else if (invalidRevendStatus(optionalTransaction.get())) {
                RevendErrorMap revendErrorMap = new RevendErrorMap();
                revendErrorMap.setClientReference(clientRef);
                revendErrorMap.setErrorMessage("Successful or Pending Transaction cannot be processed");
                revendValidationResponse.getErrorResponse().add(revendErrorMap);
            } else {
                transactionList.add(optionalTransaction.get());
            }
        }
        revendValidationResponse.setTransactionList(transactionList);
        log.info("revendValidationResponse Inside validateRevendItems method...: {}", revendValidationResponse);
        return revendValidationResponse;
    }

    private boolean invalidRevendStatus(Transaction transaction) {
        return (
            transaction.getStatus() == TransactionStatus.PENDING ||
            transaction.getStatus() == TransactionStatus.CONFIRMED ||
            transaction.getStatus() == TransactionStatus.SUCCESS
        );
    }

    public static boolean isCSVFile(MultipartFile file) {
        log.info("Inside isCSVFile with file: {}", file);
        if (Objects.nonNull(file)) {
            String fileName = file.getOriginalFilename().toLowerCase();
            log.info("Inside isCSVFile with file name: {}", fileName);
            return fileName.endsWith(".csv");
        }
        return false;
    }
}
