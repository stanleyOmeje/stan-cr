package com.stan.stancore.extended.controller.admin;

import com.google.gson.Gson;
import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.vending.extended.cron.TransactionUpdateCron;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.exception.BadRequestException;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.BulkRevendService;
import com.systemspecs.remita.vending.extended.service.TransactionService;
import com.systemspecs.remita.vending.vendingcommon.dto.request.*;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.time.LocalDate;

@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/admin/transactions")
@RestController
@RequiredArgsConstructor
public class AdminTransactionController {
    private final TransactionService transactionService;
    private final BulkRevendService bulkRevendService;
    private final TransactionUpdateCron transactionUpdateCron;
    Gson gson = new Gson();

    @ActivityTrail
    @GetMapping
    public ResponseEntity<DefaultResponse> fetchAllTransactionsByAdmin(
        @RequestParam(required = false) String productCode,
        @RequestParam(required = false) TransactionStatus status,
        @RequestParam(required = false) String userId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") String internalReference,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") String categoryCode,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") String clientReference,
        @RequestParam(required = false, defaultValue = "0", value = "page") int page,
        @RequestParam(required = false, defaultValue = "10", value = "pageSize") int pageSize
    ) {
        TransactionSearchCriteria searchCriteria = new TransactionSearchCriteria();
        searchCriteria.setProductCode(productCode);
        searchCriteria.setStatus(status);
        searchCriteria.setStartDate(start);
        searchCriteria.setEndDate(end);
        searchCriteria.setUserId(userId);
        searchCriteria.setInternalReference(internalReference);
        searchCriteria.setCategoryCode(categoryCode);
        searchCriteria.setClientReference(clientReference);

        TransactionPage transactionPage = new TransactionPage();
        transactionPage.setPageNo(page);
        transactionPage.setPageSize(pageSize);
        return transactionService.fetchAllTransactionsByAdmin(searchCriteria, transactionPage);
    }

    @ActivityTrail
    @GetMapping("/{reference}")
    public ResponseEntity<DefaultResponse> getTransactionByInternalReferenceByAdmin(@PathVariable String reference) {
        log.info(">>> Getting Transaction By InternalReference.. {}", reference);
        return transactionService.getTransactionByInternalReferenceByAdmin(reference);
    }

    @ActivityTrail
    @GetMapping("/requery/{reference}")
    public ResponseEntity<DefaultResponse> reQuery(@PathVariable String reference) {
        log.info(">>> Querying Transaction By Internal Reference.. {}", reference);
        DefaultResponse response = transactionUpdateCron.reconcileTransaction(reference);
        return ResponseEntity.ok(response);
    }

    @ActivityTrail
    @PostMapping
    public ResponseEntity<DefaultResponse> transact(@RequestBody @Valid TransactionRevendRequest transactionRequest) {
        log.info(">>> Inside TransactionController::performTransaction with Request ...{}", transactionRequest);
        DefaultResponse defaultResponse = new DefaultResponse();
        try {
            return transactionService.performRevendTransaction(transactionRequest);
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (NotFoundException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            defaultResponse.setStatus(TransactionStatus.AUTHENTICATION_ERROR.getCode());
            defaultResponse.setMessage(TransactionStatus.AUTHENTICATION_ERROR.getMessage());
            e.printStackTrace();
        }
        return ResponseEntity.ok(defaultResponse);
    }

    @ActivityTrail
    @PostMapping(value = "/re-vend")
    public ResponseEntity<DefaultResponse> reVend(@RequestBody @Valid TransactionRequest transactionRequest) {
        log.info(">>> Inside TransactionController::transact with Request: {}", transactionRequest);

        return transactionService.performRevendTransactionV2(transactionRequest, null);
    }


    @ActivityTrail
    @PostMapping(value = "bulk-revend")
    public ResponseEntity<DefaultResponse> processBulkRevendData(@RequestBody String payload) {
        log.info(">>> Inside Bulk re-vend Controller::transact with Request: {}", payload);

        BulkRevendRequest bulkRevedVRequest = gson.fromJson(payload, BulkRevendRequest.class);
        return bulkRevendService.processBulkRevend(bulkRevedVRequest);
    }

    @ActivityTrail
    @PostMapping(value = "bulk-revend-csv")
    public ResponseEntity<DefaultResponse> processBulkRevendFile(@RequestPart("file") MultipartFile file) {
        return bulkRevendService.processBulkRevendFile(file);
    }

    @ActivityTrail
    @GetMapping("query-bulk-revend/{bulkRevendReferenceId}")
    public ResponseEntity<DefaultResponse> getUploadedFile(@PathVariable String bulkRevendReferenceId, Pageable pageable) {
        log.info(">>> Geting geting UploadedFile By bulkRevendReferenceId.. {}", bulkRevendReferenceId);
        return bulkRevendService.getUploadedFile(bulkRevendReferenceId, pageable);
    }

}
