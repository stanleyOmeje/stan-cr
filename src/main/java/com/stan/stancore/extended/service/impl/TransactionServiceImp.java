package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.dto.account.AccountDebitRequest;
import com.systemspecs.remita.dto.auth.MerchantDetailsDto;
import com.systemspecs.remita.dto.auth.SubscribedServiceAccountDetailsDTO;
import com.systemspecs.remita.dto.reversal.CheckStatusResponse;
import com.systemspecs.remita.dto.reversal.FlagRequest;
import com.systemspecs.remita.enumeration.AccountType;
import com.systemspecs.remita.enumeration.CoreSDKResult;
import com.systemspecs.remita.enumeration.ServicesEnum;
import com.systemspecs.remita.enumeration.VendProcessor;
import com.systemspecs.remita.sdk.auth.CoreSDKAuth;
import com.systemspecs.remita.sdk.debit.CoreSdkAccount;
import com.systemspecs.remita.sdk.reversal.CoreSdkReversal;
import com.systemspecs.remita.vending.extended.config.VendingCoreProperties;
import com.systemspecs.remita.vending.extended.dto.CommissionDTO;
import com.systemspecs.remita.vending.extended.dto.PerformVendResponse;
import com.systemspecs.remita.vending.extended.dto.request.DisplayTransaction;
import com.systemspecs.remita.vending.extended.dto.request.ServiceMapper.TransactionMapper;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.dto.response.ListTransactionResponse;
import com.systemspecs.remita.vending.extended.dto.response.MerchantDetailsResponseDto;
import com.systemspecs.remita.vending.extended.dto.response.VendingItemListResponse;
import com.systemspecs.remita.vending.extended.dto.sms.Sms;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.AlreadyExistException;
import com.systemspecs.remita.vending.extended.exception.BadRequestException;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.exception.UnknownException;
import com.systemspecs.remita.vending.extended.service.AdminTransactionQueryService;
import com.systemspecs.remita.vending.extended.service.DisburseFundsServiceJob;
import com.systemspecs.remita.vending.extended.service.TransactionService;
import com.systemspecs.remita.vending.extended.util.*;
import com.systemspecs.remita.vending.vendingcommon.dto.request.*;
import com.systemspecs.remita.vending.vendingcommon.dto.response.*;
import com.systemspecs.remita.vending.vendingcommon.entity.*;
import com.systemspecs.remita.vending.vendingcommon.enums.*;
import com.systemspecs.remita.vending.vendingcommon.factory.VendingServiceDelegateBean;
import com.systemspecs.remita.vending.vendingcommon.flight.factory.FlightProductVendingServiceDelegateBean;
import com.systemspecs.remita.vending.vendingcommon.flight.service.FlightProductAbstractVendingService;
import com.systemspecs.remita.vending.vendingcommon.repository.*;
import com.systemspecs.remita.vending.vendingcommon.service.AbstractVendingService;
import com.systemspecs.remita.vending.vendingcommon.service.VendingServiceProcessorService;
import com.systemspecs.remita.vending.vendingcommon.util.PhoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.systemspecs.remita.vending.extended.enums.ResponseStatus.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImp implements TransactionService {

    private final FeeMappingRepository feeMappingRepository;
    private final ProductRepository productRepository;
    private final TransactionRepository transactionRepository;
    private final VendingServiceDelegateBean vendingServiceDelegateBean;
    private final VendingServiceProcessorService vendingServiceProcessorService;
    private final TransactionQueryService transactionQueryService;
    private final ExtraTokenRepository extraTokenRepository;
    private final CoreSdkAccount coreSdkAccount;
    private final CoreSDKAuth coreSDKAuth;
    private final VendingCoreProperties properties;
    private final TransactionMapper transactionMapper;
    private final CustomCommissionRepository customCommissionRepository;
    private final CoreSdkReversal coreSdkReversal;

    private final BulkVendingRepository bulkVendingRepository;
    private final VendingItemsRepository vendingItemsRepository;
    private final BulkTransactionQueryService bulkTransactionQueryService;
    private final PublishUtil publishUtil;

    private final FlightProductVendingServiceDelegateBean flightProductVendingServiceDelegateBean;

    private final SmsUtil smsUtil;
    private final MerchantNotificationConfigRepository merchantNotificationConfigRepository;
    private final AccountUtil accountUtil;
    private final DuplicateCheckRepository duplicateCheckRepository;
    private final TransactionDataRepository transactionDataRepository;
    private final FundRecoupRepository fundRecoupRepository;
    private final DisburseFundsServiceJob disburseFundsService;
    private final MerchantDetailsUtil merchantDetailsUtil;
    private final AdminTransactionQueryService queryService;

    // private static final String REF_PREFIX = "VEN";

    private static BigDecimal getAmount(TransactionRequest request, FeeMapping feeMapping, Product product) {
        if (product.getCountryCode() == null) {
            throw new NotFoundException("CountryCode for product cannot be null, update in the database");
        }

        if (product.getCountryCode().equals("NGA")) {
            return getNGAAmount(request, feeMapping);
        } else {
            return getNonNGAAmount(request, product);
        }
    }

    private static BigDecimal getNGAAmount(TransactionRequest request, FeeMapping feeMapping) {
        if (feeMapping.getFeeType() == FeeType.FIXED) {
            if (!feeMapping.getAmount().equals(request.getAmount())) {
                log.info(">>>Incorrect product amount{}{}", " expected " + feeMapping.getAmount(), INCORRECT_AMOUNT.getCode());
                throw new BadRequestException(
                    INCORRECT_AMOUNT.getMessage() + ", expected " + feeMapping.getAmount(),
                    INCORRECT_AMOUNT.getCode()
                );
            }
            return feeMapping.getAmount();
        } else {
            return validateAndGetRequestAmount(request.getAmount());
        }
    }

    private static BigDecimal getNonNGAAmount(TransactionRequest request, Product product) {
        if (product.getProductType().equals(ProductType.DYNAMIC)) {
            return validateAndGetRequestAmount(request.getAmount());
        } else {
            return null;
        }
    }

    private static BigDecimal validateAndGetRequestAmount(BigDecimal amount) {
        if (amount == null) {
            throw new BadRequestException(
                "amount " + ResponseStatus.REQUIRED_PARAMETER.getMessage(),
                ResponseStatus.REQUIRED_PARAMETER.getCode()
            );
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException(
                "amount " + ResponseStatus.INVALID_PARAMETER.getMessage(),
                ResponseStatus.INVALID_PARAMETER.getCode()
            );
        }

        return amount;
    }

    public static void main(String[] args) {
        CommissionDTO commissionDTO = new CommissionDTO();
        BigDecimal percentageComm = new BigDecimal("0.02");
        BigDecimal originalAmount = BigDecimal.valueOf(100000);
        BigDecimal minAmountCap = BigDecimal.valueOf(100);
        BigDecimal maxAmountCap = BigDecimal.valueOf(2000);

        BigDecimal commissionAmount = originalAmount.multiply(percentageComm).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalCommissionAmount = commissionAmount;
        if (minAmountCap != null) {
            if (commissionAmount.compareTo(minAmountCap) < 0 || commissionAmount.compareTo(minAmountCap) == 0) {
                finalCommissionAmount = minAmountCap;
            }
        }
        if (maxAmountCap != null) {
            if (commissionAmount.compareTo(maxAmountCap) > 0 || commissionAmount.compareTo(maxAmountCap) == 0) {
                finalCommissionAmount = maxAmountCap;
            }
        }
        BigDecimal discountedAmount = originalAmount.subtract(finalCommissionAmount).setScale(2, RoundingMode.HALF_UP);

        commissionDTO.setDiscountedAmount(discountedAmount);
        commissionDTO.setCommission(finalCommissionAmount);
    }

    @Override
    public ResponseEntity<DefaultResponse> fetchAllTransaction(TransactionPage transactionPage, TransactionSearchCriteria searchCriteria) {
        DefaultResponse response = new DefaultResponse();
        log.info(">>> Fetching all transaction using filter");

        if (transactionPage.getPageSize() > 50) {
            response.setMessage("Maximum page size exceeded");
            response.setStatus(ResponseStatus.FAILED_REQUIREMENT.getCode());
        }

        Page<Transaction> transactions = transactionQueryService.fetchAllTransactionWithFilter(transactionPage, searchCriteria);

        ListTransactionResponse listTransactionResponse = new ListTransactionResponse();
        listTransactionResponse.setTotalPage(transactions.getTotalPages());
        listTransactionResponse.setTotalContent(transactions.getTotalElements());

        List<DisplayTransaction> transactionList = transactions
            .getContent()
            .stream()
            .map(transactionMapper::mapTransactionToDisplayTransaction)
            .collect(Collectors.toList());

        log.info("transactionList inside transactionList is ...{}", transactionList);
        listTransactionResponse.setItems(transactionList);
        response.setStatus(ResponseStatus.SUCCESS.getCode());
        response.setMessage(ResponseStatus.SUCCESS.getMessage());
        response.setData(listTransactionResponse);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DefaultResponse> performTransaction(TransactionRequest request, MerchantDetailsDto merchantDetailsDto) {
        log.info(">>> Inside performTransaction");
        if (request.isBulkVending()) {
            return performBulkVendTransaction(request, merchantDetailsDto);
        } else {
            return performSingleVendTransaction(request, merchantDetailsDto);
        }
    }

    @Override
    public ResponseEntity<DefaultResponse> performTransactionV2(TransactionRequest request, MerchantDetailsDto merchantDetailsDto) {
        log.info(">>> Inside perform Transaction");

        Optional<Transaction> existingTransaction = validateAndCheckDuplicate(request);

        if (request.isBulkVending()) {
            return performBulkVendTransactionV2(request, merchantDetailsDto, existingTransaction);
        } else {
            return performSingleVendTransactionV2(request, merchantDetailsDto, existingTransaction);
        }
    }

    @Override
    public ResponseEntity<DefaultResponse> performRevendTransaction(TransactionRevendRequest request) {
        log.info(">>> Inside performRevendTransaction");
        return performSingleRevendTransaction(request);
    }

    @Override
    public ResponseEntity<DefaultResponse> performRevendTransactionV2(TransactionRequest request, MerchantDetailsDto merchantDetailsDto) {
        log.info(">>> Inside perform Re-vend Transaction with request: {}", request);
        // For re-vend we should use internal reference to check existence of original transaction
        Optional<Transaction> existingTransaction = validateReVendEligibility(request);
        return performSingleReVendTransactionV2(request, existingTransaction);
    }

    private ResponseEntity<DefaultResponse> performSingleReVendTransactionV2(
        TransactionRequest request,
        Optional<Transaction> existingTransaction
    ) {
        DefaultResponse defaultResponse = new DefaultResponse();

        Transaction tx = existingTransaction.get();
        if (tx.isSubmittedForReversals()) {
            defaultResponse.setStatus(INVALID_TRANSACTION.getCode());
            defaultResponse.setMessage("Re-vend blocked, transaction flagged for reversal");
            return ResponseEntity.ok(defaultResponse);
        }

        return reVendV2(request, tx);
    }

    @Override
    public ResponseEntity<DefaultResponse> adminRequeryTransaction(String reference) {
        log.info(">>> Inside Admin query Transaction By Payment Identifier pid ={}: ", reference);
        DefaultResponse body = new DefaultResponse();

        try {
            reference = StringUtils.trim(reference);

            Transaction transaction = transactionRepository
                .findByInternalReference(reference)
                .orElseThrow(() ->
                    new NotFoundException(
                        TransactionStatus.TRANSACTION_NOT_FOUND.getMessage(),
                        TransactionStatus.TRANSACTION_NOT_FOUND.getCode()
                    )
                );

            TransactionData txData = transactionDataRepository.findByTransaction(transaction).orElse(null);

            if (transaction.getStatus() == TransactionStatus.PENDING) {
                log.info("Status=PENDING; checking reversal...");
                checkIfReversal(transaction);
            }

            Transaction queriedTx = queryService.queryTransaction(transaction);

            transaction.setStatus(queriedTx.getStatus());
            transaction.setResponseMessage(queriedTx.getResponseMessage());
            transaction.setResponseCode(queriedTx.getResponseCode());
            transaction.setUpdatedAt(new Date());

            // If vendor provided token/units, update them
            if (queriedTx.getToken() != null) {
                transaction.setToken(queriedTx.getToken());
            }
            if (queriedTx.getUnits() != null) {
                transaction.setUnits(queriedTx.getUnits());
            }

            // Step 3: Persist updated transaction status
            transactionRepository.save(transaction);

            // 🔹 Build response DTO from the updated transaction
            QueryTransactionResponse qtr = new QueryTransactionResponse();
            qtr.setAmount(transaction.getAmount());
            qtr.setProductCode(transaction.getProductCode());
            qtr.setCategoryCode(transaction.getCategoryCode());
            qtr.setUnit(transaction.getUnits());
            qtr.setToken(transaction.getToken());
            qtr.setCreatedAt(transaction.getCreatedAt());
            qtr.setClientReference(transaction.getClientReference());
            qtr.setPaymentIdentifier(transaction.getInternalReference());
            qtr.setResponseMessage(transaction.getResponseMessage());
            qtr.setTransactionStatus(String.valueOf(transaction.getStatus()));

            if (txData != null) {
                qtr.setAccountNumber(txData.getAccountNumber());
                qtr.setPhoneNumber(txData.getPhoneNumber());
            }

            ExtraToken extraToken = extraTokenRepository.findByTransaction(transaction);
            log.info("extraToken in query Transaction By Internal Reference: {}", extraToken);
            qtr.setExtraTokens(mapMerchantExtraTokenToExtraData(extraToken));

            body.setMessage(transaction.getResponseMessage());
            body.setStatus(resolveApiStatus(transaction));
            body.setData(qtr);

            log.info("<<< Response from query Transaction: {}", body);
            return ResponseEntity.ok(body);
        } catch (BadRequestException e) {
            log.warn("Bad request: {}", e.getMessage());
            body.setStatus(e.getCode());
            body.setMessage(e.getMessage());
        } catch (NotFoundException e) {
            log.warn("Not found: {}", e.getMessage());
            body.setStatus(e.getCode());
            body.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Unhandled error in getTransactionByPaymentIdentifier", e);
            body.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
            body.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        }

        log.info("<<< Response from query transaction (error path): {}", body);
        return ResponseEntity.ok(body);
    }

    @Override
    public ResponseEntity<DefaultResponse> getTransactionByPaymentIdentifier(String paymentIdentifier, String merchantOrgId) {
        log.info(">>> getTransactionByPaymentIdentifier pid={} orgId={}", paymentIdentifier, merchantOrgId);
        DefaultResponse body = new DefaultResponse();
        try {
            paymentIdentifier = StringUtils.trim(paymentIdentifier);
            merchantOrgId = StringUtils.trim(merchantOrgId);
            Transaction transaction = transactionRepository
                .findByClientReferenceAndMerchantOrgId(paymentIdentifier, merchantOrgId)
                .orElseThrow(() ->
                    new NotFoundException(
                        TransactionStatus.INVALID_TRANSACTION_REFERENCE.getMessage(),
                        TransactionStatus.INVALID_TRANSACTION_REFERENCE.getCode()
                    )
                );

            TransactionData txData = transactionDataRepository.findByTransaction(transaction).orElse(null);

            if (transaction.getStatus() == TransactionStatus.PENDING) {
                log.info("Status=PENDING; checking reversal and querying transaction...");
                transaction = checkIfReversal(transaction);
                transaction = queryTransaction(transaction);
            }

            QueryTransactionResponse qtr = new QueryTransactionResponse();
            qtr.setAmount(transaction.getAmount());
            qtr.setProductCode(transaction.getProductCode());
            qtr.setCategoryCode(transaction.getCategoryCode());
            qtr.setUnit(transaction.getUnits());
            qtr.setToken(transaction.getToken());
            qtr.setCreatedAt(transaction.getCreatedAt());
            qtr.setClientReference(transaction.getClientReference());
            qtr.setPaymentIdentifier(transaction.getInternalReference());
            qtr.setResponseMessage(transaction.getResponseMessage());
            qtr.setTransactionStatus(String.valueOf(transaction.getStatus()));
            if (txData != null) {
                qtr.setAccountNumber(txData.getAccountNumber());
                qtr.setPhoneNumber(txData.getPhoneNumber());
            }

            ExtraToken extraToken = extraTokenRepository.findByTransaction(transaction);
            log.info("extraToken in getTransactionByInternalReference: {}", extraToken);
            qtr.setExtraTokens(mapMerchantExtraTokenToExtraData(extraToken));

            body.setMessage(transaction.getResponseMessage());
            body.setStatus(resolveApiStatus(transaction));
            body.setData(qtr);

            log.info("<<< Response from getTransactionByInternalReference: {}", body);
            return ResponseEntity.ok(body);
        } catch (BadRequestException e) {
            log.warn("Bad request: {}", e.getMessage());
            body.setStatus(e.getCode());
            body.setMessage(e.getMessage());
        } catch (NotFoundException e) {
            log.warn("Not found: {}", e.getMessage());
            body.setStatus(e.getCode());
            body.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Unhandled error in getTransactionByPaymentIdentifier", e);
            body.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
            body.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        }

        log.info("<<< Response from getTransactionByInternalReference (error path): {}", body);
        return ResponseEntity.ok(body);
    }

    private String resolveApiStatus(Transaction tx) {
        boolean isTerminalSuccess =
            tx.getStatus() == TransactionStatus.SUCCESS ||
                tx.getStatus() == TransactionStatus.CONFIRMED ||
                tx.getStatus() == TransactionStatus.COMPLETED ||
                TransactionStatus.SUCCESS.getCode().equals(tx.getResponseCode());

        if (isTerminalSuccess) {
            return TransactionStatus.SUCCESS.getCode();
        }

        if (tx.getStatus() == TransactionStatus.TRANSACTION_FAILED) {
            return TransactionStatus.TRANSACTION_FAILED.getCode();
        }
        return TransactionStatus.PENDING.getCode();
    }

    private ResponseEntity<DefaultResponse> performBulkVendTransaction(TransactionRequest request, MerchantDetailsDto merchantDetailsDto) {
        Optional<Transaction> optionalTransaction = transactionRepository.findByInternalReference(request.getClientReference());
        if (optionalTransaction.isPresent()) {
            Transaction transaction = optionalTransaction.get();
            if (transaction.getStatus() == TransactionStatus.TRANSACTION_FAILED) {
                transaction = queryTransaction(transaction);
            }
            if (transaction.getStatus() == TransactionStatus.TRANSACTION_FAILED) {
                return bulkVend(request, merchantDetailsDto);
            }
        } else {
            return bulkVend(request, merchantDetailsDto);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<DefaultResponse> performBulkVendTransactionV2(
        TransactionRequest request,
        MerchantDetailsDto merchantDetailsDto,
        Optional<Transaction> existingTransaction
    ) {
        if (existingTransaction.isEmpty()) {
            return bulkVendV2(request, merchantDetailsDto);
        }

        Transaction tx = existingTransaction.get();

        if (tx.getStatus() == TransactionStatus.TRANSACTION_FAILED) {
            tx = queryTransaction(tx);
            if (tx.getStatus() == TransactionStatus.TRANSACTION_FAILED) {
                return bulkVendV2(request, merchantDetailsDto);
            }
        }

        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<DefaultResponse> bulkVend(TransactionRequest request, MerchantDetailsDto merchantDetailsDto) {
        String clientProfileId = request.getProfileId();
        Product product = getProduct(request.getProductCode());

        request.setProductCode(product.getCode());

        String processorId = getProcessorId(request);

        AbstractVendingService vendingService = getVendingService(processorId);

        FeeMapping feeMapping = getFeeMapping(product);

        BigDecimal amount = getAmount(request, feeMapping, product);

        request.setAmount(amount);

        String internalRef = request.getInternalReference();

        log.info(">>> Calling initiate Transactions >> ");
        TransactionResponse result = vendingService.initiateTransaction(request, internalRef);

        if (result != null && result.getStatus() == TransactionStatus.SYSTEM_ERROR) {
            log.info(result.getMessage());
            throw new BadRequestException(result.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }

        if (result != null && result.getStatus() == TransactionStatus.PENDING) {
            throw new UnknownException(result.getMessage(), REQUIRED_PARAMETER.getCode());
        }

        String fallbackProcessor = null;
        TransactionResponse fallbackResult;
        String fallbackFailureMessage = null;
        String mainFailureMessage = null;
        boolean processedWithFallback = false;
        String fallbackResponseCode = null;

        if ((result == null || result.getStatus() == TransactionStatus.TRANSACTION_FAILED)) {
            if (result.isDoFallBack()) {
                fallbackProcessor = getFallbackProcessorId(request);
                log.info(" fallbackProcessor is ...{}", fallbackProcessor);
                if (fallbackProcessor != null) {
                    mainFailureMessage = result == null ? "unknown error" : result.getMessage();
                    vendingService = getVendingService(fallbackProcessor);
                    fallbackResult = vendingService.initiateTransaction(request, internalRef);
                    if (fallbackResult != null && fallbackResult.getStatus() != TransactionStatus.SUCCESS) {
                        fallbackFailureMessage = fallbackResult.getMessage();
                    }

                    if (fallbackResult != null && fallbackResult.getStatus() == TransactionStatus.SUCCESS) {
                        processedWithFallback = true;
                        result = fallbackResult;
                    }
                }
            }
        }

        TransactionDTO dto = new TransactionDTO();
        dto.setUserId(clientProfileId);
        dto.setInternalReference(internalRef);
        dto.setProcessedWithFallback(processedWithFallback);
        dto.setFallbackProcessorId(fallbackProcessor);
        dto.setFallbackResponseMessage(fallbackFailureMessage);
        dto.setMainResponseMessage(mainFailureMessage);

        //set Merchant Info
        dto.setMerchantName(merchantDetailsDto.getRegisteredBusinessName());
        dto.setMerchantOrgId(merchantDetailsDto.getOrgId());
        dto.setAccountNumber(merchantDetailsDto.getAccountNumber());
        dto.setIpAddress(merchantDetailsDto.getRequestIp());
        dto.setBankCode(merchantDetailsDto.getBankCode());
        dto.setCaller(request.getCaller());
        dto.setService("Vending");
        dto.setFundingType("");
        dto.setOrgId(merchantDetailsDto.getOrgId());

        Transaction transaction = vendingService.createTransaction(result, request, product, dto);
        log.info(">>> Creating Transaction ");
        LocalTransactionResponseForBulkVend response = new LocalTransactionResponseForBulkVend();
        response.setAmount(transaction.getAmount());
        response.setProductCode(transaction.getProductCode());
        response.setCategoryCode(transaction.getCategoryCode());
        response.setToken(transaction.getToken());
        response.setResponseMessage(transaction.getResponseMessage());
        response.setStatus(transaction.getStatus());
        response.setUnit(transaction.getUnits());
        response.setMetadata(createMetadata(transaction));
        response.setCreatedAt(transaction.getCreatedAt());
        response.setUpdatedAt(transaction.getUpdatedAt());
        response.setReference(transaction.getInternalReference());
        response.setRetrialTimeInSec(transaction.getRetrialTimeInSec());

        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setData(response);
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    private ResponseEntity<DefaultResponse> bulkVendV2(TransactionRequest request, MerchantDetailsDto merchantDetailsDto) {
        final String clientProfileId = request.getProfileId();
        final Product product = getProduct(request.getProductCode());
        request.setProductCode(product.getCode());

        final String processorId = getProcessorId(request);
        AbstractVendingService vendingService = getVendingService(processorId);

        final FeeMapping feeMapping = getFeeMapping(product);
        final BigDecimal amount = getAmount(request, feeMapping, product);
        request.setAmount(amount);

        final String internalRef = ReferenceUtil.generateInternalReference();

        log.info(">>> Calling initiate Transactions inside bulk vending >> ");
        TransactionResponse result = vendingService.initiateTransaction(request, internalRef);

        validateInitialResultOrThrow(result);

        FallbackOutcome fallback = attemptFallbackIfApplicable(result, request, internalRef);
        if (fallback.processedWithFallback) {
            vendingService = getVendingService(fallback.fallbackProcessor);
            result = fallback.result;
        } else {
            log.warn("Fallback attempt was not successful. Retaining original result [ref={}, processor={}].", internalRef, processorId);
        }

        TransactionDTO dto = buildTransactionDTO(
            clientProfileId,
            internalRef,
            fallback.processedWithFallback,
            fallback.fallbackProcessor,
            fallback.fallbackFailureMessage,
            fallback.mainFailureMessage,
            merchantDetailsDto,
            request
        );

        Transaction transaction = vendingService.createTransactionV2(result, request, product, dto, processorId, internalRef);
        log.info(">>> Building Local Response for Bulk Vending >>>>");

        LocalTransactionResponseForBulkVend local = buildLocalResponseForBulkVend(transaction, internalRef);
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setData(local);

        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    private void validateInitialResultOrThrow(TransactionResponse result) {
        if (result != null && result.getStatus() == TransactionStatus.SYSTEM_ERROR) {
            log.info(result.getMessage());
            throw new BadRequestException(result.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }
        if (result != null && result.getStatus() == TransactionStatus.PENDING) {
            throw new UnknownException(result.getMessage(), REQUIRED_PARAMETER.getCode());
        }
    }

    private FallbackOutcome attemptFallbackIfApplicable(TransactionResponse result, TransactionRequest request, String internalRef) {
        if (result == null) {
            return FallbackOutcome.none(null);
        }

        if (result.getStatus() != TransactionStatus.TRANSACTION_FAILED || !result.isDoFallBack()) {
            return FallbackOutcome.none(result);
        }

        String fallbackProcessor = getFallbackProcessorId(request);
        log.info("fallbackProcessor is ...{}", fallbackProcessor);

        if (fallbackProcessor == null) {
            return FallbackOutcome.none(result);
        }

        String mainFailureMessage = result.getMessage();
        AbstractVendingService fallbackService = getVendingService(fallbackProcessor);
        TransactionResponse fallbackResult = fallbackService.initiateTransaction(request, internalRef);

        if (fallbackResult != null && fallbackResult.getStatus() == TransactionStatus.SUCCESS) {
            return new FallbackOutcome(
                true,
                fallbackProcessor,
                null,
                fallbackResult.getProcessorResponseCode(),
                mainFailureMessage,
                fallbackResult
            );
        }

        String fallbackFailureMessage = (fallbackResult != null) ? fallbackResult.getMessage() : "unknown error";
        String fallbackResponseCode = (fallbackResult != null) ? fallbackResult.getProcessorResponseCode() : null;

        return new FallbackOutcome(false, fallbackProcessor, fallbackFailureMessage, fallbackResponseCode, mainFailureMessage, result);
    }

    private TransactionDTO buildTransactionDTO(
        String clientProfileId,
        String internalRef,
        boolean processedWithFallback,
        String fallbackProcessorId,
        String fallbackFailureMessage,
        String mainFailureMessage,
        MerchantDetailsDto merchantDetailsDto,
        TransactionRequest request
    ) {
        TransactionDTO dto = new TransactionDTO();
        dto.setUserId(clientProfileId);
        dto.setInternalReference(internalRef);
        dto.setProcessedWithFallback(processedWithFallback);
        dto.setFallbackProcessorId(fallbackProcessorId);
        dto.setFallbackResponseMessage(fallbackFailureMessage);
        dto.setMainResponseMessage(mainFailureMessage);

        dto.setMerchantName(merchantDetailsDto.getRegisteredBusinessName());
        dto.setMerchantOrgId(merchantDetailsDto.getOrgId());
        dto.setAccountNumber(merchantDetailsDto.getAccountNumber());
        dto.setIpAddress(merchantDetailsDto.getRequestIp());
        dto.setBankCode(merchantDetailsDto.getBankCode());

        dto.setCaller(request.getCaller());
        dto.setService("Vending");
        dto.setFundingType("");
        dto.setOrgId(merchantDetailsDto.getOrgId());

        return dto;
    }

    private LocalTransactionResponseForBulkVend buildLocalResponseForBulkVend(Transaction transaction, String internalRef) {
        LocalTransactionResponseForBulkVend response = new LocalTransactionResponseForBulkVend();
        response.setAmount(transaction.getAmount());
        response.setProductCode(transaction.getProductCode());
        response.setCategoryCode(transaction.getCategoryCode());
        response.setToken(transaction.getToken());
        response.setResponseMessage(transaction.getResponseMessage());
        response.setStatus(transaction.getStatus());
        response.setUnit(transaction.getUnits());
        response.setMetadata(createMetadata(transaction));
        response.setCreatedAt(transaction.getCreatedAt());
        response.setUpdatedAt(new Date());
        response.setReference(internalRef);
        response.setRetrialTimeInSec(transaction.getRetrialTimeInSec());
        return response;
    }

    private ResponseEntity<DefaultResponse> performSingleVendTransaction(
        TransactionRequest request,
        MerchantDetailsDto merchantDetailsDto
    ) {
        resolveReference(request);
        checkDuplicateTransaction(request, merchantDetailsDto);
        return vend(request, merchantDetailsDto);
    }

    private ResponseEntity<DefaultResponse> performSingleRevendTransaction(TransactionRevendRequest request) {
        Optional<Transaction> transactionToReVend = transactionRepository.findByInternalReference(request.getInternalReference());
        if (transactionToReVend.isEmpty()) {
            throw new BadRequestException(
                "transaction with client reference " + ResponseStatus.NOT_FOUND.getMessage(),
                NOT_FOUND.getCode()
            );
        } else {
            Transaction transaction = transactionToReVend.get();
            return reVend(transaction);
        }
    }

    private ResponseEntity<DefaultResponse> performSingleVendTransactionV2(
        TransactionRequest request,
        MerchantDetailsDto merchantDetailsDto,
        Optional<Transaction> existingTransaction
    ) {
        checkDuplicateTransaction(request, merchantDetailsDto);
        return vendV2(request, merchantDetailsDto, existingTransaction);
    }

    private ResponseEntity<DefaultResponse> vendV2(
        TransactionRequest request,
        MerchantDetailsDto merchantDetailsDto,
        Optional<Transaction> existingTransaction
    ) {
        final DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());

        log.info(">>> Inside VendV2 module for request: >>>>>>>>>>>> {}", request);

        final StopWatch sw = new StopWatch("vend");

        sw.start("checkDuplicateClientRef");
        if (existingTransaction.isPresent()) {
            throw new AlreadyExistException("Duplicate transaction detected for client reference: " + request.getClientReference());
        }
        sw.stop();

        try {
            sw.start("getProcessorId");
            final String processorId = getProcessorId(request);
            sw.stop();

            sw.start("getProduct");
            final Product product = getProduct(request.getProductCode());
            sw.stop();

            sw.start("resolveReference");
            resolveReference(request);
            sw.stop();

            sw.start("validateRequest");
            final ResponseEntity<DefaultResponse> validationResponse = validateRequest(request, product);
            sw.stop();
            if (Objects.nonNull(validationResponse)) {
                log.info("[TIMING] vend aborted after validation. Total={}ms\n{}", sw.getTotalTimeMillis(), sw.prettyPrint());
                return validationResponse;
            }

            MerchantDetailsResponseDto authResponse = merchantDetailsUtil.authenticateMerchant(
                merchantDetailsDto.getRequestIp(),
                merchantDetailsDto.getApiKey()
            );
            if (!"00".equals(authResponse.getStatus())) {
                defaultResponse.setStatus(authResponse.getStatus());
                defaultResponse.setMessage("Merchant authentication failed: " + authResponse.getMessage());
                return ResponseEntity.ok(defaultResponse);
            }

            if (authResponse.getData() == null) {
                throw new NotFoundException("Merchant not subscribed to vending service");
            }

            if (authResponse.getData().getBankCode() == null || StringUtils.isBlank(authResponse.getData().getAccountNumber())) {
                throw new NotFoundException("Vending service account details  and Bank Code not available.");
            }

            String billingType = authResponse.getData().getBillingType();
            String accountNumber = authResponse.getData().getAccountNumber();
            String bankCode = authResponse.getData().getBankCode();

            sw.start("generateInternalRef");
            final String internalRef = ReferenceUtil.generateInternalReference();
            sw.stop();

            sw.start("secureFund");
            secureFundV2(merchantDetailsDto, request, billingType, accountNumber, bankCode);
            sw.stop();

            sw.start("normalizeProductCode");
            request.setProductCode(product.getCode());
            sw.stop();

            sw.start("getVendingService");
            final AbstractVendingService vendingService = getVendingService(processorId);
            sw.stop();

            sw.start("getFeeMapping");
            final FeeMapping feeMapping = getFeeMapping(product);
            sw.stop();

            sw.start("computeAmount");
            final BigDecimal amount = getAmount(request, feeMapping, product);
            request.setAmount(amount);
            sw.stop();

            log.info(">>> Calling Perform Vend V2 module for request: {}", request);

            sw.start("performVend");
            final PerformVendResponse vendingResponse = performVendV2(vendingService, request, internalRef);
            final TransactionResponse result = vendingResponse.getResult();
            log.info("Perform vend result => {}", result);
            sw.stop();

            sw.start("resolveCommission");
            final CommissionDTO commission = resolveCommision(request, merchantDetailsDto, vendingResponse);
            sw.stop();

            sw.start("resolvePlatformCommission");
            final CommissionDTO platformCommission = resolvePlatformCommision(request, merchantDetailsDto, vendingResponse);
            sw.stop();

            sw.start("disburseFunds");
            disburseFundsService.disBurseFunds(merchantDetailsDto, commission, request, vendingResponse);
            sw.stop();

            sw.start("buildTransactionDto");
            final TransactionDTO dto = buildTransactionDtoV2(
                merchantDetailsDto,
                vendingResponse,
                commission,
                internalRef,
                request,
                platformCommission,
                billingType
            );
            sw.stop();

            sw.start("createTransactions");
            final Transaction transaction = createTransactionsV2(result, request, product, dto, processorId, billingType, internalRef);
            sw.stop();

            sw.start("buildLocalTransactionResponse");
            final LocalTransactionResponse response = buildLocalTransactionResponse(transaction);
            sw.stop();

            defaultResponse.setMessage(result.getMessage());
            defaultResponse.setStatus(result.getCode());
            defaultResponse.setData(response);

            log.info("[TIMING] vend completed. Total={}ms\n{}", sw.getTotalTimeMillis(), sw.prettyPrint());

            return ResponseEntity.ok(defaultResponse);
        } catch (BadRequestException e) {
            log.warn("BadRequest in vend: {}", e.getMessage());
            defaultResponse.setStatus(e.getCode()); // If your exception lacks getCode(), map here (e.g., "400")
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in vend", e);
            // Fall back to a generic failure code/message if you have one
            defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
            defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        } finally {
            if (sw.isRunning()) {
                sw.stop();
            }
            log.info("[TIMING] vend exit. Total={}ms\n{}", sw.getTotalTimeMillis(), sw.prettyPrint());
        }

        return ResponseEntity.ok(defaultResponse);
    }

    private ResponseEntity<DefaultResponse> vend(TransactionRequest request, MerchantDetailsDto merchantDetailsDto) {
        final DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        log.info(">>> Inside Vend module for request: >>>>>>>>>>>> {}", request);

        final StopWatch sw = new StopWatch("vend");

        try {
            sw.start("getProcessorId");
            final String processorId = getProcessorId(request);
            sw.stop();

            sw.start("getProduct");
            final Product product = getProduct(request.getProductCode());
            sw.stop();

            /*  sw.start("resolveReference");
            resolveReference(request);
            sw.stop();*/

            /*   sw.start("checkDuplicateClientRef");
            if (transactionRepository.findByClientReference(request.getClientReference()).isPresent()) {
                sw.stop();
                throw new BadRequestException(
                    "transaction with client reference " + ResponseStatus.ALREADY_EXIST.getMessage(),
                    ResponseStatus.ALREADY_EXIST.getCode()
                );
            }
            sw.stop(); // in case findByClientReference() returned empty quickly*/

            sw.start("validateRequest");
            final ResponseEntity<DefaultResponse> validationResponse = validateRequest(request, product);
            sw.stop();
            if (Objects.nonNull(validationResponse)) {
                log.info("[TIMING] vend aborted after validation. Total={}ms\n{}", sw.getTotalTimeMillis(), sw.prettyPrint());
                return validationResponse;
            }

            sw.start("generateInternalRef");
            final String internalRef = ReferenceUtil.generateInternalReference();
            sw.stop();

            sw.start("secureFund");
            secureFund(merchantDetailsDto, request);
            sw.stop();

            sw.start("normalizeProductCode");
            request.setProductCode(product.getCode());
            sw.stop();

            sw.start("getVendingService");
            final AbstractVendingService vendingService = getVendingService(processorId);
            sw.stop();

            sw.start("getFeeMapping");
            final FeeMapping feeMapping = getFeeMapping(product);
            sw.stop();

            sw.start("computeAmount");
            final BigDecimal amount = getAmount(request, feeMapping, product);
            request.setAmount(amount);
            sw.stop();

            log.info(">>> Calling initiateTransaction module");

            sw.start("performVend");
            final PerformVendResponse vendingResponse = performVend(vendingService, request, internalRef);
            final TransactionResponse result = vendingResponse.getResult();
            log.info("vending result => {}", result);
            sw.stop();

            sw.start("resolveCommission");
            final CommissionDTO commission = resolveCommision(request, merchantDetailsDto, vendingResponse);
            sw.stop();

            sw.start("resolvePlatformCommission");
            final CommissionDTO platformCommission = resolvePlatformCommision(request, merchantDetailsDto, vendingResponse);
            sw.stop();

            sw.start("disburseFunds");
            disburseFundsService.disBurseFunds(merchantDetailsDto, commission, request, vendingResponse);
            sw.stop();

            sw.start("buildTransactionDto");
            final TransactionDTO dto = buildTransactionDto(
                merchantDetailsDto,
                vendingResponse,
                commission,
                internalRef,
                request,
                platformCommission
            );
            sw.stop();

            sw.start("createTransactions");
            final Transaction transaction = createTransactions(result, request, product, dto, merchantDetailsDto, processorId);
            sw.stop();

            sw.start("buildLocalTransactionResponse");
            final LocalTransactionResponse response = buildLocalTransactionResponse(transaction);
            sw.stop();

            defaultResponse.setMessage(result.getMessage());
            defaultResponse.setStatus(result.getCode());
            defaultResponse.setData(response);

            log.info("[TIMING] vend completed. Total={}ms\n{}", sw.getTotalTimeMillis(), sw.prettyPrint());

            return ResponseEntity.ok(defaultResponse);
        } catch (BadRequestException e) {
            log.warn("BadRequest in vend: {}", e.getMessage());
            defaultResponse.setStatus(e.getCode()); // If your exception lacks getCode(), map here (e.g., "400")
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in vend", e);
            // Fall back to a generic failure code/message if you have one
            defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
            defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        } finally {
            if (sw.isRunning()) {
                sw.stop();
            }
            log.info("[TIMING] vend exit. Total={}ms\n{}", sw.getTotalTimeMillis(), sw.prettyPrint());
        }

        return ResponseEntity.ok(defaultResponse);
    }

    private ResponseEntity<DefaultResponse> vendOld(TransactionRequest request, MerchantDetailsDto merchantDetailsDto) {
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        try {
            String processorId = getProcessorId(request);
            Product product = getProduct(request.getProductCode());
            resolveReference(request);
            if (transactionRepository.findByClientReference(request.getClientReference()).isPresent()) {
                throw new BadRequestException(
                    "transaction with client reference " + ResponseStatus.ALREADY_EXIST.getMessage(),
                    ResponseStatus.ALREADY_EXIST.getCode()
                );
            }
            ResponseEntity<DefaultResponse> validationResponse = validateRequest(request, product);
            if (Objects.nonNull(validationResponse)) {
                return validationResponse;
            }
            String internalRef = ReferenceUtil.generateInternalReference();

            secureFund(merchantDetailsDto, request);

            request.setProductCode(product.getCode());

            AbstractVendingService vendingService = getVendingService(processorId);

            FeeMapping feeMapping = getFeeMapping(product);

            BigDecimal amount = getAmount(request, feeMapping, product);

            request.setAmount(amount);

            log.info(">>> Calling initiateTransaction module");

            PerformVendResponse vendingResponse = performVend(vendingService, request, internalRef);
            TransactionResponse result;
            result = vendingResponse.getResult();
            log.info("vending result => {}", result);

            CommissionDTO commission = resolveCommision(request, merchantDetailsDto, vendingResponse);

            CommissionDTO platformCommission = resolvePlatformCommision(request, merchantDetailsDto, vendingResponse);

            disburseFundsService.disBurseFunds(merchantDetailsDto, commission, request, vendingResponse);
            TransactionDTO dto = buildTransactionDto(
                merchantDetailsDto,
                vendingResponse,
                commission,
                internalRef,
                request,
                platformCommission
            );

            Transaction transaction = createTransactions(result, request, product, dto, merchantDetailsDto, processorId);

            LocalTransactionResponse response = buildLocalTransactionResponse(transaction);

            defaultResponse.setMessage(result.getMessage());
            defaultResponse.setStatus(result.getCode());
            defaultResponse.setData(response);
            return ResponseEntity.ok(defaultResponse);
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok(defaultResponse);
    }

    public String resolveProcessor(PerformVendResponse vendingResponse, TransactionRequest request) {
        String vendProcessor = vendingResponse.isProcessedWithFallback() ? vendingResponse.getFallbackProcessor() : getProcessorId(request);
        log.info("vendProcessor is ....{}", vendProcessor);
        return vendProcessor;
    }

    public CommissionDTO resolveCommision(
        TransactionRequest request,
        MerchantDetailsDto merchantDetailsDto,
        PerformVendResponse vendingResponse
    ) {
        String vendProcessor = resolveProcessor(vendingResponse, request);
        CommissionDTO commission = resolveAmountWithCommission(request, merchantDetailsDto, vendProcessor);
        log.info("commission is ....{}", commission);
        return commission;
    }

    public CommissionDTO resolvePlatformCommision(
        TransactionRequest request,
        MerchantDetailsDto merchantDetailsDto,
        PerformVendResponse vendingResponse
    ) {
        String vendProcessor = resolveProcessor(vendingResponse, request);
        CommissionDTO commission = resolveAmountWithPlatformCommission(request, merchantDetailsDto, vendProcessor);
        log.info("platform commission is ....{}", commission);
        return commission;
    }

    public String secureFund(MerchantDetailsDto merchantDetailsDto, TransactionRequest request) {
        if (merchantDetailsDto == null) {
            throw new BadRequestException("Merchant not found", NOT_FOUND.getCode());
        }
        String subscriptionType = SubscriptionUtil.getSubscriptionType(merchantDetailsDto);
        boolean requiresServiceAccount = SubscriptionUtil.getRequiresServiceAccount(merchantDetailsDto);
        log.info("requiresServiceAccount on Merchant is => {}", requiresServiceAccount);
        if (Strings.isBlank(subscriptionType)) {
            throw new BadRequestException("Subscription type cannot be blank", NOT_FOUND.getCode());
        }
        //if prepaid, hold fund
        if (subscriptionType.equalsIgnoreCase(String.valueOf(SubscriptionType.PREPAID))) {
            AccountDebitRequest accountHoldRequest = getAccountDebitRequest(
                merchantDetailsDto,
                request.getAmount(),
                request.getClientReference()
            );
            holdOriginalTransactionAmount(accountHoldRequest);

            log.info(
                "Merchant with profileId {}, and account number {}, held successfully by amount: {}",
                merchantDetailsDto.getOrgId(),
                merchantDetailsDto.getAccountNumber(),
                request.getAmount()
            );
        }
        return subscriptionType;
    }

    public void secureFundV2(
        MerchantDetailsDto merchantDetailsDto,
        TransactionRequest request,
        String billingType,
        String accountNumber,
        String bankCode
    ) {
        if (StringUtils.equalsIgnoreCase(billingType, SubscriptionType.PREPAID.name())) {
            AccountDebitRequest accountHoldRequest = getAccountDebitRequestV2(
                merchantDetailsDto,
                request.getAmount(),
                request.getClientReference(),
                accountNumber,
                bankCode
            );

            holdOriginalTransactionAmount(accountHoldRequest);

            log.info(
                "Merchant with profileId {}, has billing type {}, held successfully for amount: {}",
                merchantDetailsDto.getOrgId(),
                billingType,
                request.getAmount()
            );
        } else {
            log.info("Merchant with profileId {} has billing type {}, no fund hold required.", merchantDetailsDto.getOrgId(), billingType);
        }
    }

    public TransactionDTO buildTransactionDtoV2(
        MerchantDetailsDto merchantDetailsDto,
        PerformVendResponse vendingResponse,
        CommissionDTO commission,
        String internalRef,
        TransactionRequest request,
        CommissionDTO platformCommission,
        String billingType
    ) {
        TransactionDTO dto = new TransactionDTO();
        dto.setUserId(merchantDetailsDto.getProfileId());
        dto.setInternalReference(internalRef);
        dto.setProcessedWithFallback(vendingResponse.isProcessedWithFallback());
        dto.setFallbackProcessorId(vendingResponse.getFallbackProcessor());
        dto.setFallbackResponseMessage(vendingResponse.getFallbackFailureMessage());
        dto.setMainResponseMessage(vendingResponse.getMainFailureMessage());

        //set Merchant Info
        dto.setMerchantName(merchantDetailsDto.getRegisteredBusinessName());
        dto.setMerchantOrgId(merchantDetailsDto.getOrgId());
        dto.setAccountNumber(merchantDetailsDto.getAccountNumber());
        dto.setIpAddress(merchantDetailsDto.getRequestIp());
        dto.setBankCode(merchantDetailsDto.getBankCode());
        dto.setSubscriptionType(billingType);
        dto.setTenantId(merchantDetailsDto.getTenantId());
        if (Objects.nonNull(commission)) {
            dto.setDiscountedAmount(commission.getDiscountedAmount());
            dto.setCommission(commission.getCommission());
        }
        dto.setCaller(request.getCaller());
        dto.setServiceName("VENDING");
        dto.setService("VENDING");
        dto.setFundingType(billingType);
        dto.setOrgId(merchantDetailsDto.getOrgId());

        dto.setMerchantPercentageCommission(commission.getMerchantPercentageCommission());
        dto.setMerchantMinAmount(commission.getMerchantMinAmount());
        dto.setMerchantMaxAmount(commission.getMerchantMaxAmount());
        dto.setPlatformCommission(platformCommission.getPlatformCommission());
        dto.setPlatformPercentageCommission(platformCommission.getPlatformPercentageCommission());
        dto.setPlatformMinAmount(platformCommission.getPlatformMinAmount());
        dto.setPlatformMaxAmount(platformCommission.getPlatformMaxAmount());

        return dto;
    }

    public TransactionDTO buildTransactionDto(
        MerchantDetailsDto merchantDetailsDto,
        PerformVendResponse vendingResponse,
        CommissionDTO commission,
        String internalRef,
        TransactionRequest request,
        CommissionDTO platformCommission
    ) {
        TransactionDTO dto = new TransactionDTO();
        String subscriptionType = SubscriptionUtil.getSubscriptionType(merchantDetailsDto);
        dto.setUserId(merchantDetailsDto.getProfileId());
        dto.setInternalReference(internalRef);
        dto.setProcessedWithFallback(vendingResponse.isProcessedWithFallback());
        dto.setFallbackProcessorId(vendingResponse.getFallbackProcessor());
        dto.setFallbackResponseMessage(vendingResponse.getFallbackFailureMessage());
        dto.setMainResponseMessage(vendingResponse.getMainFailureMessage());

        //set Merchant Info
        dto.setMerchantName(merchantDetailsDto.getRegisteredBusinessName());
        dto.setMerchantOrgId(merchantDetailsDto.getOrgId());
        dto.setAccountNumber(merchantDetailsDto.getAccountNumber());
        dto.setIpAddress(merchantDetailsDto.getRequestIp());
        dto.setBankCode(merchantDetailsDto.getBankCode());
        dto.setSubscriptionType(subscriptionType);
        dto.setTenantId(merchantDetailsDto.getTenantId());
        if (Objects.nonNull(commission)) {
            dto.setDiscountedAmount(commission.getDiscountedAmount());
            dto.setCommission(commission.getCommission());
        }
        dto.setCaller(request.getCaller());
        dto.setServiceName("VENDING");
        dto.setService("VENDING");
        dto.setFundingType(subscriptionType);
        dto.setOrgId(merchantDetailsDto.getOrgId());

        dto.setMerchantPercentageCommission(commission.getMerchantPercentageCommission());
        dto.setMerchantMinAmount(commission.getMerchantMinAmount());
        dto.setMerchantMaxAmount(commission.getMerchantMaxAmount());
        dto.setPlatformCommission(platformCommission.getPlatformCommission());
        dto.setPlatformPercentageCommission(platformCommission.getPlatformPercentageCommission());
        dto.setPlatformMinAmount(platformCommission.getPlatformMinAmount());
        dto.setPlatformMaxAmount(platformCommission.getPlatformMaxAmount());

        return dto;
    }

    public LocalTransactionResponse buildLocalTransactionResponse(Transaction transaction) {
        LocalTransactionResponse response = new LocalTransactionResponse();
        response.setAmount(transaction.getAmount());
        response.setProductCode(transaction.getProductCode());
        response.setCategoryCode(transaction.getCategoryCode());
        response.setToken(transaction.getToken());
        response.setExtraTokens(getExtraTokens(transaction));
        response.setUnit(transaction.getUnits() != null ? transaction.getUnits() : "");
        response.setMetadata(createMetadata(transaction));
        response.setCreatedAt(transaction.getCreatedAt());
        response.setUpdatedAt(transaction.getUpdatedAt());
        response.setPaymentIdentifier(transaction.getInternalReference());
        response.setClientReference(transaction.getClientReference());
        response.setRetrialTimeInSec(transaction.getRetrialTimeInSec());

        return response;
    }

    public Transaction createTransactionsV2(
        TransactionResponse result,
        TransactionRequest request,
        Product product,
        TransactionDTO dto,
        String processorId,
        String billingType,
        String internalRef
    ) {
        AbstractVendingService vendingService = getVendingService(processorId);
        Transaction transaction = vendingService.createTransactionV2(result, request, product, dto, processorId, internalRef);
        log.info(">>>> Checking Billing type for POSTPAID Transactions >>>>>>");
        if (("POSTPAID").equalsIgnoreCase(billingType) && result.getStatus().equals(TransactionStatus.SUCCESS)) {
            vendingService.createPostpaidTransaction(request, dto, product);
        }
        return transaction;
    }

    public Transaction createTransactions(
        TransactionResponse result,
        TransactionRequest request,
        Product product,
        TransactionDTO dto,
        MerchantDetailsDto merchantDetailsDto,
        String processorId
    ) {
        AbstractVendingService vendingService = getVendingService(processorId);
        Transaction transaction = vendingService.createTransaction(result, request, product, dto);
        log.info(">>>> Creating Transaction  ");
        String subscriptionType = SubscriptionUtil.getSubscriptionType(merchantDetailsDto);
        if (subscriptionType.equalsIgnoreCase("POSTPAID") && result.getStatus().equals(TransactionStatus.SUCCESS)) {
            vendingService.createPostpaidTransaction(request, dto, product);
        }
        return transaction;
    }

    private FlagRequest buildFlagReversalRequest(
        AccountDebitRequest accountDebitRequest,
        PerformVendResponse vendHereResponse,
        String processorId,
        TransactionRequest request,
        MerchantDetailsDto merchantDetailsDto,
        String internalRef,
        BigDecimal amount
    ) {
        FlagRequest flagRequest = new FlagRequest();
        flagRequest.setMerchantAccountNumber(accountDebitRequest.getAccountNumber());
        flagRequest.setMerchantBankCode(accountDebitRequest.getBankCode());
        flagRequest.setTransactionRef(accountDebitRequest.getDebitRef());
        flagRequest.setMerchantBankName(merchantDetailsDto.getBankCode());
        flagRequest.setAmount(amount);
        flagRequest.setPaymentChannel("CASH");
        flagRequest.setResponseCode(vendHereResponse.getResult().getCode());
        flagRequest.setResponseMessage(vendHereResponse.getTransactionStatus());
        flagRequest.setProcessorId(processorId);
        flagRequest.setTransactionType("VENDING");
        flagRequest.setOrgId(merchantDetailsDto.getOrgId());
        flagRequest.setProcessorReference(internalRef);
        flagRequest.setServiceName("VENDING");
        return flagRequest;
    }

    private PerformVendResponse performVendV2(AbstractVendingService vendingService, TransactionRequest request, String internalRef) {
        PerformVendResponse response = new PerformVendResponse();
        log.info("Performing Vending with processor [{}] for reference {}", vendingService.getProcessorId(), request.getClientReference());

        TransactionResponse mainResult = null;
        try {
            mainResult = vendingService.initiateTransaction(request, internalRef);
            if (isSuccess(mainResult)) {
                response.setResult(mainResult);
                response.setTransactionStatus(TransactionStatus.SUCCESS.name());
                response.setProcessedWithFallback(false);
                return response;
            }
            response.setMainFailureMessage(mainResult != null ? mainResult.getMessage() : "Main processor returned null");
        } catch (Exception ex) {
            log.error(
                "Exception in main processor [{}] for reference {}: {}",
                vendingService.getProcessorId(),
                request.getClientReference(),
                ex.getMessage(),
                ex
            );
            response.setMainFailureMessage("Exception: " + ex.getMessage());
        }

        return attemptFallback(request, internalRef, mainResult, response);
    }

    private PerformVendResponse attemptFallback(
        TransactionRequest request,
        String internalRef,
        TransactionResponse mainResult,
        PerformVendResponse response
    ) {
        String fallbackProcessorId = getFallbackProcessorId(request);
        log.info("Calling attempt Fallback processor with id ...{} for reference {}", fallbackProcessorId, request.getClientReference());
        if (fallbackProcessorId == null) {
            response.setTransactionStatus(TransactionStatus.FALL_BACK_PROCESSOR_FAILED.name());
            response.setProcessedWithFallback(false);
            return response;
        }
        log.info("FallBack Performing Vending with processor [{}] for reference {}", fallbackProcessorId, request.getClientReference());

        try {
            AbstractVendingService fallbackService = getVendingService(fallbackProcessorId);
            TransactionResponse fallbackResult = fallbackService.initiateTransaction(request, internalRef);

            response.setFallbackProcessor(fallbackProcessorId);
            response.setFallbackResult(fallbackResult);
            response.setFallbackResponseCode(fallbackResult != null ? fallbackResult.getProcessorResponseCode() : null);

            if (isSuccess(fallbackResult)) {
                response.setResult(fallbackResult);
                response.setTransactionStatus(TransactionStatus.SUCCESS.name());
                response.setProcessedWithFallback(true);
            } else {
                response.setFallbackFailureMessage(fallbackResult != null ? fallbackResult.getMessage() : "Fallback returned null");
                response.setTransactionStatus(TransactionStatus.FALL_BACK_PROCESSOR_FAILED.name());
                response.setProcessedWithFallback(true);
            }
        } catch (Exception ex) {
            log.error(
                "Exception in fallback processor [{}] for reference {}: {}",
                fallbackProcessorId,
                request.getClientReference(),
                ex.getMessage(),
                ex
            );
            response.setFallbackProcessor(fallbackProcessorId);
            response.setFallbackFailureMessage("Exception: " + ex.getMessage());
            response.setTransactionStatus(TransactionStatus.FALL_BACK_PROCESSOR_FAILED.name());
            response.setProcessedWithFallback(true);
        }

        return response;
    }

    private boolean isSuccess(TransactionResponse txResponse) {
        return txResponse != null && txResponse.getStatus() == TransactionStatus.SUCCESS;
    }

    private PerformVendResponse performVend(AbstractVendingService vendingService, TransactionRequest request, String internalRef) {
        TransactionResponse result = vendingService.initiateTransaction(request, internalRef);
        PerformVendResponse performVendResponse = new PerformVendResponse();
        performVendResponse.setTransactionStatus("SUCCESS");

        if (result != null && result.getStatus() == TransactionStatus.SYSTEM_ERROR) {
            log.info(result.getMessage());
            performVendResponse.setTransactionStatus(TransactionStatus.SYSTEM_ERROR.name());
        }
        String fallbackProcessor = null;
        TransactionResponse fallbackResult = null;
        String fallbackFailureMessage = null;
        String mainFailureMessage = null;
        boolean processedWithFallback = false;
        String fallbackResponseCode = null;

        if ((result == null || result.getStatus() == TransactionStatus.TRANSACTION_FAILED)) {
            performVendResponse.setTransactionStatus(TransactionStatus.TRANSACTION_FAILED.name());
            if (result.isDoFallBack()) {
                fallbackProcessor = getFallbackProcessorId(request);
                log.info("fallbackProcessor is ...{}", fallbackProcessor);
                if (fallbackProcessor != null) {
                    mainFailureMessage = result == null ? "unknown error" : result.getMessage();
                    vendingService = getVendingService(fallbackProcessor);
                    fallbackResult = vendingService.initiateTransaction(request, internalRef);
                    if (fallbackResult != null && fallbackResult.getStatus() != TransactionStatus.SUCCESS) {
                        fallbackFailureMessage = fallbackResult.getMessage();
                        fallbackResponseCode = fallbackResult.getProcessorResponseCode();
                        performVendResponse.setTransactionStatus(TransactionStatus.TRANSACTION_FAILED.getMessage());
                    }

                    if (fallbackResult != null && fallbackResult.getStatus() == TransactionStatus.SUCCESS) {
                        processedWithFallback = true;
                        result = fallbackResult;
                    }
                }
            }
        }
        performVendResponse.setResult(result);
        performVendResponse.setFallbackResponseCode(fallbackResponseCode);
        performVendResponse.setFallbackProcessor(fallbackProcessor);
        performVendResponse.setFallbackResult(fallbackResult);
        performVendResponse.setFallbackFailureMessage(fallbackFailureMessage);
        performVendResponse.setMainFailureMessage(mainFailureMessage);
        performVendResponse.setProcessedWithFallback(processedWithFallback);
        if (result != null) {
            performVendResponse.setTransactionStatus(result.getStatus().name());
        }
        return performVendResponse;
    }

    private void holdOriginalTransactionAmount(AccountDebitRequest accountDebitRequest) {
        CoreSDKResult holdResult = coreSdkAccount.holdAmount(accountDebitRequest);
        if (holdResult == CoreSDKResult.INSUFFICIENT_FUNDS) {
            throw new BadRequestException(TransactionStatus.INSUFFICIENT_FUND.getMessage(), TransactionStatus.INSUFFICIENT_FUND.getCode());
        } else if (holdResult == CoreSDKResult.FAILED) {
            throw new BadRequestException("Transaction Failed", TransactionStatus.TRANSACTION_FAILED.getCode());
        }
    }

    private CoreSDKResult releaseAndDebitFund(AccountDebitRequest accountDebitRequest, VendProcessor processor) {
        CoreSDKResult releaseAndDebitFundResult = coreSdkAccount.doVendingPrepaidReleaseDebit(accountDebitRequest, processor);
        return releaseAndDebitFundResult;
    }

    private CoreSDKResult releaseHeldFund(AccountDebitRequest accountDebitRequest) {
        CoreSDKResult releaseFundResult = coreSdkAccount.releaseAmount(accountDebitRequest);
        return releaseFundResult;
    }

    private ResponseEntity<DefaultResponse> debitHere(AccountDebitRequest accountDebitRequest, String processorId) {
        CoreSDKResult debitResult = coreSdkAccount.doPrepaidVendingDebit(
            accountDebitRequest,
            VendProcessor.valueOf(processorId.toUpperCase())
        );

        if (debitResult == CoreSDKResult.FAILED) {
            DefaultResponse response = new DefaultResponse();
            response.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
            response.setMessage("Debit Unsuccessful: Insufficient Fund");

            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        return null;
    }

    private ResponseEntity<DefaultResponse> validateRequest(TransactionRequest request, Product product) {
        DefaultResponse defaultResponse = new DefaultResponse();
        String phoneNumber = request.getData().getPhoneNumber();
        String countryCode = product.getCountryCode();
        if (Strings.isBlank(request.getData().getAccountNumber())) {
            DefaultResponse response = new DefaultResponse();
            response.setStatus(NOT_FOUND.getCode());
            response.setMessage("UniqueIdentifier Not Found");
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        if (!PhoneUtil.isValidPhoneNumber(countryCode, phoneNumber)) {
            log.info("Invalid phone number: country code = {}, number = {}", countryCode, phoneNumber);
            defaultResponse.setStatus(BAD_REQUEST.getCode());
            defaultResponse.setMessage("Invalid Phone Number");
            return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
        }
        return null;
    }

    private void resolveReference(TransactionRequest request) {
        if (StringUtils.isNotBlank(request.getClientReference())) {
            log.info("{-} Allow Vending");
        } else if (StringUtils.isNotBlank(request.getPaymentIdentifier())) {
            request.setClientReference(request.getPaymentIdentifier());
        } else {
            throw new BadRequestException("Reference cannot be blank{}", REQUIRED_PARAMETER.getCode());
        }
    }

    private AccountDebitRequest getAccountDebitRequest(MerchantDetailsDto merchantDetailsDto, BigDecimal amount, String clientReference) {
        AccountDebitRequest accountDebitRequest = new AccountDebitRequest();

        boolean requiresServiceAccount = SubscriptionUtil.getRequiresServiceAccount(merchantDetailsDto);
        log.info("requiresServiceAccount on Merchant is => {}", requiresServiceAccount);
        if (!requiresServiceAccount) {
            log.info("Enter because service account is NOT required, Main account is required");
            if (StringUtils.isBlank(merchantDetailsDto.getAccountNumber()) || StringUtils.isBlank(merchantDetailsDto.getBankCode())) {
                throw new NotFoundException("Vending service account details not available ");
            }
            accountDebitRequest.setAccountNumber(merchantDetailsDto.getAccountNumber());
            accountDebitRequest.setBankCode(merchantDetailsDto.getBankCode());
        } else {
            log.info("Enter because service account is required.");
            Optional<SubscribedServiceAccountDetailsDTO> accountDetailsDTO = coreSDKAuth.getSubscribedServiceAccount(
                merchantDetailsDto,
                ServicesEnum.VENDING
            );

            if (accountDetailsDTO.isEmpty()) {
                throw new NotFoundException("Vending service account details not available ");
            }
            merchantDetailsDto.setAccountNumber(accountDetailsDTO.get().getAccountNumber());
            merchantDetailsDto.setBankCode(accountDetailsDTO.get().getBankCode());
            accountDebitRequest.setAccountNumber(accountDetailsDTO.get().getAccountNumber());
            accountDebitRequest.setBankCode(accountDetailsDTO.get().getBankCode());
        }
        accountDebitRequest.setAccountType(AccountType.PREPAID);
        accountDebitRequest.setAmount(amount);
        accountDebitRequest.setDebitRef(clientReference);
        accountDebitRequest.setServiceName("VENDING");
        accountDebitRequest.setOrgId(merchantDetailsDto.getOrgId());
        return accountDebitRequest;
    }

    private AccountDebitRequest getAccountDebitRequestV2(
        MerchantDetailsDto merchantDetailsDto,
        BigDecimal amount,
        String clientReference,
        String accountNumber,
        String bankCode
    ) {
        AccountDebitRequest accountDebitRequest = new AccountDebitRequest();
        accountDebitRequest.setAccountNumber(accountNumber);
        accountDebitRequest.setBankCode(bankCode);
        accountDebitRequest.setAccountType(AccountType.PREPAID);
        accountDebitRequest.setAmount(amount);
        accountDebitRequest.setDebitRef(clientReference);
        accountDebitRequest.setServiceName("VENDING");
        accountDebitRequest.setOrgId(merchantDetailsDto.getOrgId());
        return accountDebitRequest;
    }

    private ExtraData getExtraTokens(Transaction transaction) {
        log.info("inside getExtraTokens");
        ExtraData extraData = new ExtraData();
        if (transaction.getExtraToken() == null) {
            return null;
        } else {
            resolveExtraToken(transaction, extraData);
            return extraData;
        }
    }

    public ResponseEntity<DefaultResponse> reVend(Transaction transaction) {
        log.info(">>> Inside reVend");
        final DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        try {
            if (transaction == null) {
                throw new BadRequestException("Transaction cannot be null", REQUIRED_PARAMETER.getCode());
            }
            // 1) Query original transaction status from its original processor
            AbstractVendingService originalService = getVendingService(transaction.getProcessorId());
            TransactionResponse queried = originalService.queryTransaction(transaction);
            log.info(
                ">>> originalService.queryTransaction internalReference {} and response {} ",
                transaction.getInternalReference(),
                queried
            );
            if (queried == null) {
                throw new BadRequestException("Transaction cannot be reVended " + ResponseStatus.NOT_FOUND, NOT_FOUND.getCode());
            }
            // 2) Determine re-vend processor + request
            final String internalRef = ReferenceUtil.generateInternalReference();
            final String productCode = transaction.getProductCode();
            final String processorId = getProcessorIdForReVend(productCode);
            AbstractVendingService reVendService = getVendingService(processorId);
            final TransactionRequest request = buildRevendRequest(transaction, internalRef);
            // 3) If APPROVED, skip initiate and go straight to resolve path
            if (TransactionStatus.SUCCESS.equals(queried.getStatus()) || TransactionStatus.APPROVED.equals(queried.getStatus())) {
                log.info("Query Status returned successfully");
                return processAndRespond(transaction, queried, reVendService, processorId, request, defaultResponse);
            }

            // 4) Otherwise, initiate re-vend
            log.info(">>> Calling initiateTransaction");
            TransactionResponse initiated = reVendService.initiateTransaction(request, internalRef);

            if (initiated != null && initiated.getStatus() == TransactionStatus.SYSTEM_ERROR) {
                log.info(initiated.getMessage());
                throw new BadRequestException(initiated.getMessage(), INTERNAL_SERVER_ERROR.getCode());
            }
            if (initiated != null && initiated.getStatus() == TransactionStatus.PENDING) {
                throw new UnknownException(initiated.getMessage(), REQUIRED_PARAMETER.getCode());
            }

            // 5) Resolve, notify, update, respond
            return processAndRespond(transaction, initiated, reVendService, processorId, request, defaultResponse);
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
            log.warn("reVend BadRequestException: code={}, msg={}", e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("reVend unexpected error", e);
            // Keep default failed response
        }
        return ResponseEntity.ok(defaultResponse);
    }

    /**
     * Build a TransactionRequest for re-vend using existing transaction + stored data.
     */
    private TransactionRequest buildRevendRequest(Transaction transaction, String internalRef) {
        final TransactionRequest req = new TransactionRequest();
        req.setAmount(transaction.getAmount());
        req.setInternalReference(internalRef);
        req.setProductCode(transaction.getProductCode());
        req.setClientReference(transaction.getClientReference());
        req.setPaymentIdentifier(transaction.getClientReference());
        TransactionData txData = transactionDataRepository.findByTransaction(transaction).orElse(null);
        final TransactionDataRequest data = new TransactionDataRequest();
        if (txData != null) {
            data.setAccountNumber(txData.getAccountNumber());
            data.setPhoneNumber(txData.getPhoneNumber());
        }
        req.setData(data);
        return req;
    }

    /**
     * Common happy-path:
     * - send notification
     * - update transaction for re-vend
     * - postpaid follow-up
     * - build response + publish tokens
     */
    private ResponseEntity<DefaultResponse> processAndRespond(
        Transaction transaction,
        TransactionResponse transactionResponse,
        AbstractVendingService reVendService,
        String processorId,
        TransactionRequest request,
        DefaultResponse defaultResponse
    ) {
        sendNotification(transactionResponse, request, transaction);
        transaction = reVendService.updateTransactionForReVend(transaction, transactionResponse, processorId, request);
        log.info(">>> Updating Transaction");
        if ("POSTPAID".equalsIgnoreCase(transaction.getSubscriptionType()) && TransactionStatus.SUCCESS.equals(transaction.getStatus())) {
            reVendService.createPostpaidTransactionFromRevend(transaction);
        }
        LocalTransactionResponse local = buildLocalTransactionResponse(transaction);
        defaultResponse.setMessage(transactionResponse.getMessage());
        defaultResponse.setStatus(transactionResponse.getCode());
        defaultResponse.setData(local);
        ExtraData extraData = getExtraTokens(transaction);
        ExtraToken extraToken = convertExtraDataToExtraToken(extraData, transaction.getToken());
        publishUtil.publishVendNotification(extraToken, transaction.getClientReference());
        return ResponseEntity.ok(defaultResponse);
    }

    public ResponseEntity<DefaultResponse> reVendV2(TransactionRequest request, Transaction tx) {
        log.info(">>> Re-Vend (status sync) with request: {} and transaction: {}", request, tx);

        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());

        try {
            if (tx == null) {
                throw new BadRequestException("Transaction not found", NOT_FOUND.getCode());
            }

            request.setAmount(tx.getAmount());

            AbstractVendingService vendingService = getVendingService(tx.getProcessorId());
            TransactionResponse transactionResponse = vendingService.queryTransaction(tx);

            if (transactionResponse == null) {
                throw new BadRequestException("Transaction status unavailable", NOT_FOUND.getCode());
            }

            String processorId = getProcessorIdForReVend(tx.getProductCode());
            tx = vendingService.updateTransactionForReVend(tx, transactionResponse, processorId, request);
            log.info("Local transaction status updated to: {}", transactionResponse.getStatus());

            LocalTransactionResponse localResponse = buildLocalTransactionResponse(tx);
            defaultResponse.setMessage(transactionResponse.getMessage());
            defaultResponse.setStatus(transactionResponse.getCode());
            defaultResponse.setData(localResponse);

            if (TransactionStatus.TRANSACTION_FAILED.equals(transactionResponse.getStatus())) {
                defaultResponse.setMessage(transactionResponse.getMessage());
                defaultResponse.setStatus(transactionResponse.getCode());
                log.info("Transaction FAILED on provider side, synced locally and returning.");
                return ResponseEntity.ok(defaultResponse);
            }

            if (TransactionStatus.PENDING.equals(transactionResponse.getStatus())) {
                defaultResponse.setMessage(transactionResponse.getMessage());
                defaultResponse.setStatus(transactionResponse.getCode());
                log.info("Transaction is still PENDING on provider side, returning status only.");
                return ResponseEntity.ok(defaultResponse);
            }

            if (TransactionStatus.SUCCESS.equals(transactionResponse.getStatus())) {
                log.info("Transaction SUCCESSFUL on provider side, applying success side effects.");

                sendNotification(transactionResponse, request, tx);

                if (SubscriptionType.POSTPAID.name().equalsIgnoreCase(tx.getSubscriptionType())) {
                    vendingService.createPostpaidTransactionFromRevend(tx);
                }

                ExtraData extraData = getExtraTokens(tx);
                ExtraToken extraToken = convertExtraDataToExtraToken(extraData, tx.getToken());
                publishUtil.publishVendNotification(extraToken, tx.getClientReference());

                defaultResponse.setMessage(transactionResponse.getMessage());
                defaultResponse.setStatus(transactionResponse.getCode());

                log.info("Re-Vend success side effects completed.");
                return ResponseEntity.ok(defaultResponse);
            }

            log.warn("Transaction returned unknown status: {}", transactionResponse.getStatus());
            return ResponseEntity.ok(defaultResponse);
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during reVend", e);
            defaultResponse.setMessage("An error occurred while processing reVend");
            defaultResponse.setStatus(TransactionStatus.SYSTEM_ERROR.getCode());
        }

        return ResponseEntity.ok(defaultResponse);
    }

    private TransactionResponse resolveResponse(
        TransactionResponse transactionResponse,
        TransactionRequest request,
        AbstractVendingService vendingService,
        String internalRef
    ) {
        log.info(
            "inside resolveResponse with TransactionResponse...{} ...TransactionRequest ...{}" +
                "vendingService...{} and ... internalRef...{}",
            transactionResponse,
            request,
            vendingService,
            internalRef
        );
        String fallbackProcessor = null;
        TransactionResponse fallbackResult;
        String fallbackFailureMessage = null;
        String mainFailureMessage = null;
        boolean processedWithFallback = false;
        String fallbackResponseCode = null;

        if ((transactionResponse == null || transactionResponse.getStatus() == TransactionStatus.TRANSACTION_FAILED)) {
            if (transactionResponse.isDoFallBack()) {
                fallbackProcessor = getFallbackProcessorId(request);
                log.info(" fallbackProcessor is ...{}", fallbackProcessor);
                if (fallbackProcessor != null) {
                    mainFailureMessage = transactionResponse == null ? "unknown error" : transactionResponse.getMessage();
                    vendingService = getVendingService(fallbackProcessor);
                    fallbackResult = vendingService.initiateTransaction(request, internalRef);
                    if (fallbackResult != null && fallbackResult.getStatus() != TransactionStatus.SUCCESS) {
                        fallbackFailureMessage = fallbackResult.getMessage();
                        fallbackResponseCode = fallbackResult.getProcessorResponseCode();
                    }

                    if (fallbackResult != null && fallbackResult.getStatus() == TransactionStatus.SUCCESS) {
                        processedWithFallback = true;
                        transactionResponse = fallbackResult;
                    }
                }
            }
        }
        return transactionResponse;
    }

    private void sendNotification(TransactionResponse transactionResponse, TransactionRequest request, Transaction optionalTransaction) {
        if (transactionResponse != null && transactionResponse.getStatus() == TransactionStatus.SUCCESS) {
            log.info("Inside sendNotification of reVend transaction");
            String token = transactionResponse.getToken();
            log.info("revended token is ...{}", token);
            if (StringUtils.isNotBlank(token)) {
                Optional<MerchantNotificationConfig> merchantNotificationConfig = merchantNotificationConfigRepository.findFirstByMerchantId(
                    optionalTransaction.getMerchantOrgId()
                );
                if (merchantNotificationConfig.isPresent() && merchantNotificationConfig.get().isEnableSms()) {
                    // send sms
                    Sms sms = Sms.builder().mobileNumber(request.getData().getPhoneNumber()).token(token).build();
                    log.info("about to send sms with payload ...{}", sms);
                    smsUtil.sendTokenSms(sms);
                    log.info("After sending sms ...");
                }
            }
        }
    }

    private ExtraToken convertExtraDataToExtraToken(ExtraData extraData, String token) {
        if (extraData == null) {
            ExtraToken extraToken = new ExtraToken();
            extraToken.setStandardTokenValue(token);
            return extraToken;
        }
        ExtraToken extraToken = new ExtraToken();

        extraToken.setKct1(extraData.getKct1());
        extraToken.setKct2(extraData.getKct2());
        extraToken.setBsstTokenValue(extraData.getBsstTokenValue());
        extraToken.setStandardTokenValue(extraData.getStandardTokenValue());
        if (StringUtils.isBlank(extraData.getStandardTokenValue())) {
            extraToken.setStandardTokenValue(token);
        }
        extraToken.setBsstTokenUnits(extraData.getBsstTokenUnits());
        extraToken.setStandardTokenUnits(extraData.getStandardTokenUnits());
        extraToken.setPin(extraData.getPin());
        extraToken.setMeterNumber(extraData.getMeterNumber());
        extraToken.setCustomerName(extraData.getCustomerName());
        extraToken.setReceiptNumber(extraData.getReceiptNumber());
        extraToken.setTariffClass(extraData.getTariffClass());
        extraToken.setAmountPaid(extraData.getAmountPaid());
        extraToken.setCostOfUnit(extraData.getCostOfUnit());
        extraToken.setAmountForDebt(extraData.getAmountForDebt());
        extraToken.setUnitsType(extraData.getUnitsType());
        extraToken.setAccountBalance(extraData.getAccountBalance());
        extraToken.setMapToken1(extraData.getMapToken1());
        extraToken.setMapToken2(extraData.getMapToken2());
        extraToken.setMapUnits(extraData.getMapUnits());
        extraToken.setTariffRate(extraData.getTariffRate());
        extraToken.setAddress(extraData.getAddress());
        extraToken.setVat(extraData.getVat());
        extraToken.setUnitsPurchased(extraData.getUnitsPurchased());
        extraToken.setMessage(extraData.getMessage());
        extraToken.setAccountType(extraData.getAccountType());
        extraToken.setMinVendAmount(extraData.getMinVendAmount());
        extraToken.setMaxVendAmount(extraData.getMaxVendAmount());
        extraToken.setRemainingDebt(extraData.getRemainingDebt());
        extraToken.setMeterType(extraData.getMeterType());
        extraToken.setReplacementCost(extraData.getReplacementCost());
        extraToken.setOutstandingDebt(extraData.getOutstandingDebt());
        extraToken.setAdministrativeCharge(extraData.getAdministrativeCharge());
        extraToken.setFixedCharge(extraData.getFixedCharge());
        extraToken.setLossOfRevenue(extraData.getLossOfRevenue());
        extraToken.setPenalty(extraData.getPenalty());
        extraToken.setMeterServiceCharge(extraData.getMeterServiceCharge());
        extraToken.setMeterCost(extraData.getMeterCost());
        extraToken.setApplicationFee(extraData.getApplicationFee());
        extraToken.setReadingText(extraData.getReadingText());
        extraToken.setCBTRegistrationCharge(extraData.getCBTRegistrationCharge());
        extraToken.setCBTExaminationCharge(extraData.getCBTExaminationCharge());
        extraToken.setOptionalMock(extraData.getOptionalMock());
        extraToken.setStrisBrilliant(extraData.getStrisBrilliant());

        return extraToken;
    }

    public void validateReference(Optional<Transaction> optionalTransaction) {
        // Optional<Transaction> optionalTransaction = transactionRepository.findByClientReference(request.getClientReference());
        if (optionalTransaction.isPresent()) {
            TransactionStatus status = optionalTransaction.get().getStatus();
            if (
                status.equals(TransactionStatus.SUCCESS) ||
                    status.equals(TransactionStatus.CONFIRMED) ||
                    status.equals(TransactionStatus.COMPLETED) ||
                    status.equals(TransactionStatus.PENDING)
            ) {
                throw new BadRequestException(
                    "Cannot reVend transaction that is successful, confirmed, completed, or pending. " + FAILED_REQUIREMENT.getMessage(),
                    FAILED_REQUIREMENT.getCode()
                );
            }
        }
    }

    @Override
    public ResponseEntity<DefaultResponse> getTransactionByReference(String internalReference, String merchantOrgId) {
        log.info(">>> Inside getTransactionByReference ... {} and Merchant OrgId...{}", internalReference, merchantOrgId);
        DefaultResponse defaultResponse = new DefaultResponse();
        try {
            internalReference = StringUtils.trim(internalReference);
            merchantOrgId = StringUtils.trim(merchantOrgId);
            Optional<Transaction> optionalTransaction = transactionRepository.findByInternalReferenceAndMerchantOrgId(
                internalReference,
                merchantOrgId
            );
            if (optionalTransaction.isEmpty()) {
                optionalTransaction = transactionRepository.findByClientReferenceAndMerchantOrgId(internalReference, merchantOrgId);
                if (optionalTransaction.isEmpty()) {
                    throw new NotFoundException(
                        TransactionStatus.INVALID_TRANSACTION_REFERENCE.getMessage(),
                        TransactionStatus.INVALID_TRANSACTION_REFERENCE.getCode()
                    );
                }
            }

            Transaction transaction = optionalTransaction.get();
            TransactionData transactionData = transactionDataRepository.findByTransaction(transaction).get();
            log.info("Transaction data: {}", transactionData);
            if (transaction.getStatus() == TransactionStatus.PENDING) {
                log.info("enter because status was PENDING)");
                transaction = checkIfReversal(transaction);
                transaction = queryTransaction(transaction);
            }

            QueryTransactionResponse response = new QueryTransactionResponse();
            response.setAmount(transaction.getAmount());
            response.setProductCode(transaction.getProductCode());
            response.setCategoryCode(transaction.getCategoryCode());
            response.setUnit(transaction.getUnits());
            response.setToken(transaction.getToken());

            ExtraToken extraToken = extraTokenRepository.findByTransaction(transaction);
            log.info("extraToken in getTransactionByInternalReference : {}", extraToken);
            response.setExtraTokens(mapMerchantExtraTokenToExtraData(extraToken));
            response.setCreatedAt(transaction.getCreatedAt());
            response.setClientReference(transaction.getClientReference());
            response.setPaymentIdentifier(transaction.getInternalReference());
            response.setResponseMessage(transaction.getResponseMessage());
            response.setTransactionStatus(String.valueOf(transaction.getStatus()));
            if (transactionData != null) {
                response.setAccountNumber(transactionData.getAccountNumber());
                response.setPhoneNumber(transactionData.getPhoneNumber());
            }
            defaultResponse.setMessage(transaction.getResponseMessage());
            if (
                TransactionStatus.SUCCESS == transaction.getStatus() ||
                    TransactionStatus.CONFIRMED == transaction.getStatus() ||
                    TransactionStatus.COMPLETED == transaction.getStatus() ||
                    TransactionStatus.SUCCESS.getCode().equals(transaction.getResponseCode())
            ) {
                defaultResponse.setStatus(TransactionStatus.SUCCESS.getCode());
            } else if (TransactionStatus.TRANSACTION_FAILED == transaction.getStatus()) {
                defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
            } else {
                defaultResponse.setStatus(TransactionStatus.PENDING.getCode());
            }
            defaultResponse.setData(response);
            log.info("<<< Response from getTransactionByInternalReference : {}", defaultResponse);
            return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (NotFoundException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
            defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
            e.printStackTrace();
        }
        log.info("<<< Response from getTransactionByInternalReference : {}", defaultResponse);
        return ResponseEntity.ok(defaultResponse);
    }

    private Transaction checkIfReversal(Transaction transaction) {
        if (transaction.isSubmittedForReversals()) {
            if (!transaction.isReversalSuccessful()) {
                CheckStatusResponse checkStatusResponse = coreSdkReversal.checkReversalStatus(transaction.getClientReference());
                if (!checkStatusResponse.getReversalStatus().equalsIgnoreCase("REVERSED")) {
                    log.info("Reversal Status is not REVERSED");
                    transaction.setResponseCode(TransactionStatus.REVERSAL_FAILED.getCode());
                    transaction.setResponseCode(TransactionStatus.REVERSAL_FAILED.getMessage());
                } else {
                    transaction.setReversalSuccessful(true);
                    transaction.setResponseCode(TransactionStatus.REVERSAL_SUCCESSFUL.getCode());
                    transaction.setResponseCode(TransactionStatus.REVERSAL_SUCCESSFUL.getMessage());
                    transactionRepository.save(transaction);
                }
            }
        }
        return transaction;
    }

    @Override
    public ResponseEntity<DefaultResponse> fetchAllTransactionsByAdmin(TransactionSearchCriteria searchCriteria, TransactionPage page) {
        log.info(">>> Fetching all transaction by admin using filter");
        Page<Transaction> transactions = transactionQueryService.fetchAllTransactionWithFilter(page, searchCriteria);
        DefaultResponse response = new DefaultResponse();

        if (page.getPageSize() > 50) {
            response.setMessage("Maximum page size exceeded");
            response.setStatus(ResponseStatus.FAILED_REQUIREMENT.getCode());
        }

        Map<String, Object> mapData = new HashMap<>();
        mapData.put("totalPage", transactions.getTotalPages());
        mapData.put("totalContent", transactions.getTotalElements());
        mapData.put("items", transactions.getContent());
        response.setStatus(ResponseStatus.SUCCESS.getCode());
        response.setMessage(ResponseStatus.SUCCESS.getMessage());
        response.setData(mapData);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private Transaction queryTransaction(Transaction transaction) {
        String subscriptionType = transaction.getSubscriptionType();
        String processorId = transaction.getProcessedWithFallback() != null && transaction.getProcessedWithFallback()
            ? transaction.getFallbackProcessorId()
            : transaction.getProcessorId();
        String systemProductType = transaction.getCategoryCode();

        if (SystemProduct.FLIGHT.name().equalsIgnoreCase(systemProductType)) {
            FlightProductAbstractVendingService flightService = getFlightProductVendingService(processorId);
            TransactionResponse transactionResponse = flightService.queryTransaction(transaction);
            if (transactionResponse == null) {
                return transaction;
            }
            if (TransactionStatus.TRANSACTION_FAILED.getCode().equals(transactionResponse.getCode())) {
                log.info(transactionResponse.getMessage());
                if (subscriptionType.equalsIgnoreCase(String.valueOf(SubscriptionType.PREPAID))) {
                    AccountDebitRequest accountReleaseRequest = accountUtil.getQueryAccountReaseRequest(transaction);
                    CoreSDKResult releaseHeldFund = releaseHeldFund(accountReleaseRequest);
                    log.info("releaseHeldFund on Merchant for flight is => {}", releaseHeldFund);
                    if (!CoreSDKResult.RELEASED.equals(releaseHeldFund)) {
                        FundRecoup fundRecoup = accountUtil.getHoldFundRecoup(transaction);
                        fundRecoupRepository.save(fundRecoup);
                    }
                }
                return transaction;
            }
            if (transactionResponse.getStatus() == TransactionStatus.SUCCESS) {
                log.info("enter because query is successful");
                if (subscriptionType.equalsIgnoreCase(String.valueOf(SubscriptionType.PREPAID))) {
                    log.info("enter because query for flight has prepaid subscription");
                    AccountDebitRequest accountDebitRequest = accountUtil.getQueryAccountDebitRequest(transaction);
                    String vendProcessor = transaction.getProcessorId();
                    CoreSDKResult releaseAndDebitResultMessage = releaseAndDebitFund(
                        accountDebitRequest,
                        VendProcessor.valueOf(vendProcessor.toUpperCase())
                    );
                    log.info("releaseAndDebitResultMessage for flight is ....{}", releaseAndDebitResultMessage);
                    if (releaseAndDebitResultMessage == CoreSDKResult.RELEASED) {
                        FundRecoup fundRecoup = accountUtil.getReleasedFundRecoup(transaction);
                        fundRecoupRepository.save(fundRecoup);
                    } else if (releaseAndDebitResultMessage == CoreSDKResult.FAILED) {
                        FundRecoup fundRecoup = accountUtil.getFailedFundRecoup(transaction);
                        fundRecoupRepository.save(fundRecoup);
                    }
                }
            }
            return flightService.updateTransaction(transaction, transactionResponse);
        }

        AbstractVendingService vendingService = getVendingService(processorId);
        TransactionResponse transactionResponse = vendingService.queryTransaction(transaction);
        log.info("Query result in core is => {}", transactionResponse);

        if (transactionResponse == null) {
            return transaction;
        }

        if (transactionResponse.getStatus() == TransactionStatus.SYSTEM_ERROR) {
            log.info(transactionResponse.getMessage());
            return transaction;
        }

        if (transactionResponse.getStatus() == TransactionStatus.TRANSACTION_FAILED) {
            log.info("enter because query is NOT successful");
            if (subscriptionType.equalsIgnoreCase(String.valueOf(SubscriptionType.PREPAID))) {
                AccountDebitRequest accountReleaseRequest = accountUtil.getQueryAccountReaseRequest(transaction);
                CoreSDKResult releaseHeldFund = releaseHeldFund(accountReleaseRequest);
                log.info("release Held Fund on Merchant in query is => {}", releaseHeldFund);
                if (!CoreSDKResult.RELEASED.equals(releaseHeldFund)) {
                    FundRecoup fundRecoup = accountUtil.getHoldFundRecoup(transaction);
                    fundRecoupRepository.save(fundRecoup);
                }
            }
            return vendingService.updateTransaction(transaction, transactionResponse);
        }

        if (transactionResponse.getStatus() == TransactionStatus.SUCCESS) {
            log.info("enter because query is successful");
            if (subscriptionType.equalsIgnoreCase(String.valueOf(SubscriptionType.PREPAID))) {
                log.info("enter because query has prepaid subscription");
                AccountDebitRequest accountDebitRequest = accountUtil.getQueryAccountDebitRequest(transaction);
                String vendProcessor = transaction.getProcessorId();
                //release held fund and debit
                CoreSDKResult releaseAndDebitResultMessage = releaseAndDebitFund(
                    accountDebitRequest,
                    VendProcessor.valueOf(vendProcessor.toUpperCase())
                );
                log.info("release And DebitResult Message is ....{}", releaseAndDebitResultMessage);
                if (releaseAndDebitResultMessage == CoreSDKResult.RELEASED) {
                    FundRecoup fundRecoup = accountUtil.getReleasedFundRecoup(transaction);
                    fundRecoupRepository.save(fundRecoup);
                } else if (releaseAndDebitResultMessage == CoreSDKResult.FAILED) {
                    FundRecoup fundRecoup = accountUtil.getFailedFundRecoup(transaction);
                    fundRecoupRepository.save(fundRecoup);
                }
            }
        }

        return vendingService.updateTransaction(transaction, transactionResponse);
    }

    @Override
    public ResponseEntity<DefaultResponse> getTransactionByInternalReferenceByAdmin(String reference) {
        log.info(">>> Inside getTransactionByInternalReferenceByAdmin ... {}", reference);
        DefaultResponse defaultResponse = new DefaultResponse();
        try {
            Optional<Transaction> optionalTransaction = transactionRepository.findByInternalReference(reference);
            if (optionalTransaction.isEmpty()) {
                optionalTransaction = transactionRepository.findByClientReference(reference);
                if (optionalTransaction.isEmpty()) {
                    throw new NotFoundException(
                        TransactionStatus.INVALID_TRANSACTION_REFERENCE.getMessage(),
                        TransactionStatus.INVALID_TRANSACTION_REFERENCE.getCode()
                    );
                }
            }

            Transaction transaction = optionalTransaction.get();
            TransactionData transactionData = transactionDataRepository.findByTransaction(transaction).get();
            log.info("Transaction data: {}", transactionData);
            if (transaction.getStatus() == TransactionStatus.PENDING) {
                log.info("enter because status was PENDING)");
                transaction = checkIfReversal(transaction);
                transaction = queryTransaction(transaction);
            }

            QueryTransactionResponse response = new QueryTransactionResponse();
            response.setAmount(transaction.getAmount());
            response.setProductCode(transaction.getProductCode());
            response.setCategoryCode(transaction.getCategoryCode());
            response.setUnit(transaction.getUnits());
            response.setToken(transaction.getToken());

            ExtraToken extraToken = extraTokenRepository.findByTransaction(transaction);
            log.info("extraToken in getTransactionByInternalReferenceByAdmin : {}", extraToken);
            response.setExtraTokens(mapMerchantExtraTokenToExtraData(extraToken));
            response.setCreatedAt(transaction.getCreatedAt());
            response.setClientReference(transaction.getClientReference());
            response.setPaymentIdentifier(transaction.getInternalReference());
            response.setResponseMessage(transaction.getResponseMessage());
            response.setTransactionStatus(String.valueOf(transaction.getStatus()));
            if (transactionData != null) {
                response.setAccountNumber(transactionData.getAccountNumber());
                response.setPhoneNumber(transactionData.getPhoneNumber());
            }
            defaultResponse.setMessage(transaction.getResponseMessage());
            if (
                TransactionStatus.SUCCESS == transaction.getStatus() ||
                    TransactionStatus.CONFIRMED == transaction.getStatus() ||
                    TransactionStatus.COMPLETED == transaction.getStatus() ||
                    TransactionStatus.SUCCESS.getCode().equals(transaction.getResponseCode())
            ) {
                defaultResponse.setStatus(TransactionStatus.SUCCESS.getCode());
            } else if (TransactionStatus.TRANSACTION_FAILED == transaction.getStatus()) {
                defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
            } else {
                defaultResponse.setStatus(TransactionStatus.PENDING.getCode());
            }
            defaultResponse.setData(response);
            log.info("<<< Response from getTransactionByInternalReferenceByAdmin : {}", defaultResponse);
            return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (NotFoundException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
            defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
            e.printStackTrace();
        }
        log.info("<<< Response from getTransactionByInternalReferenceByAdmin : {}", defaultResponse);
        return ResponseEntity.ok(defaultResponse);
    }

    @Override
    public ResponseEntity<DefaultResponse> getBulkTransactionByClientReference(
        BulkTransactionPage bulkTransactionPage,
        BulkTransactionSearchCriteria bulkTransactionSearchCriteria
    ) {
        log.info(
            "Inside getBulkTransactionByClientReference service with client reference...{}",
            bulkTransactionSearchCriteria.getBulkClientReference()
        );
        DefaultResponse response = new DefaultResponse();
        if (Strings.isBlank(bulkTransactionSearchCriteria.getBulkClientReference())) {
            throw new NotFoundException("bulkClientReference " + NOT_FOUND.getMessage(), NOT_FOUND.getCode());
        }
        String reference = bulkTransactionSearchCriteria.getBulkClientReference();
        Optional<BulkVending> optionalBulkVending = bulkVendingRepository.findByClientReference(reference);
        if (optionalBulkVending.isEmpty()) {
            log.info("BulkVending does not exist for rrr {} ", reference);
            throw new NotFoundException("transaction " + NOT_FOUND.getMessage(), NOT_FOUND.getCode());
        }

        if (bulkTransactionPage.getPageSize() > 50) {
            response.setMessage("Maximum page size exceeded");
            response.setStatus(ResponseStatus.FAILED_REQUIREMENT.getCode());
        }

        long successCount = vendingItemsRepository.countByBulkClientReferenceAndVendStatus(reference, TransactionStatus.SUCCESS);
        long failedCount = vendingItemsRepository.countByBulkClientReferenceAndVendStatus(reference, TransactionStatus.TRANSACTION_FAILED);
        long pendingCount = vendingItemsRepository.countByBulkClientReferenceAndVendStatus(reference, TransactionStatus.PENDING);
        Page<VendingItems> bulkTransactions = bulkTransactionQueryService.fetchBulkTransactionWithFilter(
            bulkTransactionPage,
            bulkTransactionSearchCriteria
        );

        VendingItemListResponse listTransactionResponse = new VendingItemListResponse();
        listTransactionResponse.setTotalPage(bulkTransactions.getTotalPages());
        listTransactionResponse.setFailedCount(failedCount);
        listTransactionResponse.setSuccessCount(successCount);
        listTransactionResponse.setPendingCount(pendingCount);
        listTransactionResponse.setTotalContent(bulkTransactions.getTotalElements());

        List<BulkQueryResponse> transactionList = bulkTransactions
            .getContent()
            .stream()
            .map(this::mapVendingItemsToBulkQueryResponse)
            .collect(Collectors.toList());

        log.info("transactionList inside getBulkTransactionByClientReference is ...{}", transactionList);
        listTransactionResponse.setItems(transactionList);
        response.setStatus(ResponseStatus.SUCCESS.getCode());
        response.setMessage(ResponseStatus.SUCCESS.getMessage());
        response.setData(listTransactionResponse);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private FeeMapping getFeeMapping(Product product) {
        log.info(">>> Getting FeeMapping from product:{}", product);
        Optional<FeeMapping> optionalFeeMapping = feeMappingRepository.findFirstByProductCode(product.getCode());
        if (optionalFeeMapping.isEmpty()) {
            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }

        return optionalFeeMapping.get();
    }

    private String getProcessorId(TransactionRequest request) {
        log.info(">>> Getting getProcessorId from TransactionRequest:{}", request.getProductCode());
        String processorId = vendingServiceProcessorService.getProcessorId(request.getProductCode());
        if (processorId == null) {
            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }
        return processorId;
    }

    private String getProcessorIdForReVend(String productCode) {
        log.info(">>> Getting getProcessorId from TransactionRequest:{}", productCode);
        String processorId = vendingServiceProcessorService.getProcessorId(productCode);
        if (processorId == null) {
            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }
        return processorId;
    }

    private String getFallbackProcessorId(TransactionRequest request) {
        return vendingServiceProcessorService.getFallbackProcessorId(request.getProductCode());
    }

    public Product getProduct(String code) {
        log.info(">>> Getting Product from code:{}", code);
        return productRepository
            .findByCode(code)
            .orElseThrow(() -> new NotFoundException("product " + ResponseStatus.NOT_FOUND.getMessage()));
    }

    private AbstractVendingService getVendingService(String processorId) {
        log.info(">>> Getting VendingService from processorId):{}", processorId);
        AbstractVendingService serviceBean = vendingServiceDelegateBean.getDelegate(processorId);
        if (Objects.isNull(serviceBean)) {
            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }
        return serviceBean;
    }

    private ArrayList<TransactionMetaData> createMetadata(Transaction transaction) {
        TransactionMetaData transactionMetaData = new TransactionMetaData();
        transactionMetaData.setAccountNumber(transaction.getTransactionData().getAccountNumber());
        transactionMetaData.setPhoneNumber(transaction.getTransactionData().getPhoneNumber());
        return new ArrayList<>(Collections.singletonList(transactionMetaData));
    }

    private ExtraData mapExtraTokenToExtraData(ExtraToken extraToken) {
        ExtraData extraData = new ExtraData();
        if (extraToken == null) {
            return null;
        }
        extraData.setKct1(extraToken.getKct1() != null ? extraToken.getKct1() : "");
        extraData.setKct2(extraToken.getKct2() != null ? extraToken.getKct2() : "");
        extraData.setBsstTokenValue(extraToken.getBsstTokenValue() != null ? extraToken.getBsstTokenValue() : "");
        extraData.setStandardTokenValue(extraToken.getStandardTokenValue() != null ? extraToken.getStandardTokenValue() : "");
        extraData.setBsstTokenUnits(extraToken.getBsstTokenUnits() != null ? extraToken.getBsstTokenUnits() : "");
        extraData.setStandardTokenUnits(extraToken.getStandardTokenUnits() != null ? extraToken.getStandardTokenUnits() : "");
        extraData.setPin(extraToken.getPin() != null ? extraToken.getPin() : "");
        extraData.setMeterNumber(extraToken.getMeterNumber() != null ? extraToken.getMeterNumber() : "");
        extraData.setCustomerName(extraToken.getCustomerName() != null ? extraToken.getCustomerName() : "");
        extraData.setAmountPaid(extraToken.getAmountPaid() != null ? extraToken.getAmountPaid() : BigDecimal.ZERO);
        extraData.setAccountBalance(extraToken.getAccountBalance() != null ? extraToken.getAccountBalance() : BigDecimal.ZERO);
        extraData.setUnitsPurchased(extraToken.getUnitsPurchased() != null ? extraToken.getUnitsPurchased() : "");
        extraData.setMeterType(extraToken.getMeterType() != null ? extraToken.getMeterType() : "");

        extraData.setReceiptNumber(extraToken.getReceiptNumber() != null ? extraToken.getReceiptNumber() : "");
        extraData.setTariffClass(extraToken.getTariffClass() != null ? extraToken.getTariffClass() : "");

        extraData.setCostOfUnit(extraToken.getCostOfUnit() != null ? extraToken.getCostOfUnit() : BigDecimal.ZERO);
        extraData.setAmountForDebt(extraToken.getAmountForDebt() != null ? extraToken.getAmountForDebt() : BigDecimal.ZERO);
        extraData.setUnitsType(extraToken.getUnitsType() != null ? extraToken.getUnitsType() : "");

        extraData.setMapToken1(extraToken.getMapToken1() != null ? extraToken.getMapToken1() : "");
        extraData.setMapToken2(extraToken.getMapToken2() != null ? extraToken.getMapToken2() : "");
        extraData.setMapUnits(extraToken.getMapUnits() != null ? extraToken.getMapUnits() : "");
        extraData.setTariffRate(extraToken.getTariffRate() != null ? extraToken.getTariffRate() : "");
        extraData.setAddress(extraToken.getAddress() != null ? extraToken.getAddress() : "");
        extraData.setVat(extraToken.getVat() != null ? extraToken.getVat() : BigDecimal.ZERO);
        extraData.setMessage(extraToken.getMessage() != null ? extraToken.getMessage() : "");

        return extraData;
    }

    private QueryExtraData mapMerchantExtraTokenToExtraData(ExtraToken extraToken) {
        QueryExtraData extraData = new QueryExtraData();
        if (extraToken == null) {
            return null;
        }
        extraData.setKct1(extraToken.getKct1() != null ? extraToken.getKct1() : null);
        extraData.setKct2(extraToken.getKct2() != null ? extraToken.getKct2() : null);
        extraData.setBsstTokenValue(extraToken.getBsstTokenValue() != null ? extraToken.getBsstTokenValue() : null);
        extraData.setStandardTokenValue(extraToken.getStandardTokenValue() != null ? extraToken.getStandardTokenValue() : null);
        extraData.setBsstTokenUnits(extraToken.getBsstTokenUnits() != null ? extraToken.getBsstTokenUnits() : null);
        extraData.setStandardTokenUnits(extraToken.getStandardTokenUnits() != null ? extraToken.getStandardTokenUnits() : null);
        extraData.setPin(extraToken.getPin() != null ? extraToken.getPin() : null);
        extraData.setMeterNumber(extraToken.getMeterNumber());
        extraData.setCustomerName(extraToken.getCustomerName() != null ? extraToken.getCustomerName() : null);
        extraData.setAmountPaid(extraToken.getAmountPaid() != null ? extraToken.getAmountPaid() : null);
        extraData.setAccountBalance(extraToken.getAccountBalance() != null ? extraToken.getAccountBalance() : null);
        extraData.setUnitsPurchased(extraToken.getUnitsPurchased() != null ? extraToken.getUnitsPurchased() : null);
        extraData.setMeterType(extraToken.getMeterType() != null ? extraToken.getMeterType() : null);

        extraData.setAddress(extraToken.getAddress());
        extraData.setVat(extraToken.getVat());
        extraData.setMessage(extraToken.getMessage());
        extraData.setRemainingDebt(extraToken.getRemainingDebt());
        extraData.setTariffClass(extraToken.getTariffClass());
        extraData.setCostOfUnit(extraToken.getCostOfUnit());
        extraData.setAccountType(extraToken.getAccountType());
        extraData.setTariffRate(extraToken.getTariffRate());

        return extraData;
    }

    private void resolveExtraToken(Transaction transaction, ExtraData extraData) {
        extraData.setBsstTokenValue(
            transaction.getExtraToken().getBsstTokenValue() != null ? transaction.getExtraToken().getBsstTokenValue() : null
        );
        extraData.setStandardTokenValue(
            transaction.getExtraToken().getStandardTokenValue() != null ? transaction.getExtraToken().getStandardTokenValue() : null
        );
        extraData.setBsstTokenUnits(
            transaction.getExtraToken().getBsstTokenUnits() != null ? transaction.getExtraToken().getBsstTokenUnits() : null
        );
        extraData.setStandardTokenUnits(
            transaction.getExtraToken().getStandardTokenUnits() != null ? transaction.getExtraToken().getStandardTokenUnits() : null
        );
        extraData.setKct1(transaction.getExtraToken().getKct1() != null ? transaction.getExtraToken().getKct1() : null);
        extraData.setKct2(transaction.getExtraToken().getKct2() != null ? transaction.getExtraToken().getKct2() : null);
        extraData.setPin(transaction.getExtraToken().getPin() != null ? transaction.getExtraToken().getPin() : null);
        extraData.setCustomerName(
            transaction.getExtraToken().getCustomerName() != null ? transaction.getExtraToken().getCustomerName() : null
        );
        extraData.setMeterNumber(
            transaction.getExtraToken().getMeterNumber() != null ? transaction.getExtraToken().getMeterNumber() : null
        );
        extraData.setTariffClass(
            transaction.getExtraToken().getTariffClass() != null ? transaction.getExtraToken().getTariffClass() : null
        );
        extraData.setAmountPaid(transaction.getExtraToken().getAmountPaid() != null ? transaction.getExtraToken().getAmountPaid() : null);
        extraData.setCostOfUnit(transaction.getExtraToken().getCostOfUnit() != null ? transaction.getExtraToken().getCostOfUnit() : null);
        extraData.setAmountForDebt(
            transaction.getExtraToken().getAmountForDebt() != null ? transaction.getExtraToken().getAmountForDebt() : null
        );
        extraData.setUnitsType(transaction.getExtraToken().getUnitsType() != null ? transaction.getExtraToken().getUnitsType() : null);
        extraData.setAccountBalance(
            transaction.getExtraToken().getAccountBalance() != null ? transaction.getExtraToken().getAccountBalance() : null
        );
        extraData.setMessage(transaction.getExtraToken().getMessage() != null ? transaction.getExtraToken().getMessage() : null);
        extraData.setMapToken1(transaction.getExtraToken().getMapToken1() != null ? transaction.getExtraToken().getMapToken1() : null);
        extraData.setMapToken2(transaction.getExtraToken().getMapToken2() != null ? transaction.getExtraToken().getMapToken2() : null);
        extraData.setMapUnits(transaction.getExtraToken().getMapUnits() != null ? transaction.getExtraToken().getMapUnits() : null);
        extraData.setReceiptNumber(
            transaction.getExtraToken().getReceiptNumber() != null ? transaction.getExtraToken().getReceiptNumber() : null
        );
        extraData.setTariffRate(transaction.getExtraToken().getTariffRate() != null ? transaction.getExtraToken().getTariffRate() : null);
        extraData.setVat(transaction.getExtraToken().getVat() != null ? transaction.getExtraToken().getVat() : null);
        extraData.setAddress(transaction.getExtraToken().getAddress() != null ? transaction.getExtraToken().getAddress() : null);
        extraData.setMeterType(transaction.getExtraToken().getMeterType() != null ? transaction.getExtraToken().getMeterType() : null);
        extraData.setMinVendAmount(
            transaction.getExtraToken().getMinVendAmount() != null ? transaction.getExtraToken().getMinVendAmount() : null
        );
        extraData.setMaxVendAmount(
            transaction.getExtraToken().getMaxVendAmount() != null ? transaction.getExtraToken().getMaxVendAmount() : null
        );
        extraData.setAccountType(
            transaction.getExtraToken().getAccountType() != null ? transaction.getExtraToken().getAccountType() : null
        );
        extraData.setUnitsPurchased(
            transaction.getExtraToken().getUnitsPurchased() != null ? transaction.getExtraToken().getUnitsPurchased() : null
        );
        extraData.setRemainingDebt(
            transaction.getExtraToken().getRemainingDebt() != null ? transaction.getExtraToken().getRemainingDebt() : null
        );
        extraData.setReplacementCost(
            transaction.getExtraToken().getReplacementCost() != null ? transaction.getExtraToken().getReplacementCost() : null
        );
        extraData.setOutstandingDebt(
            transaction.getExtraToken().getOutstandingDebt() != null ? transaction.getExtraToken().getOutstandingDebt() : null
        );
        extraData.setAdministrativeCharge(
            transaction.getExtraToken().getAdministrativeCharge() != null ? transaction.getExtraToken().getAdministrativeCharge() : null
        );
        extraData.setFixedCharge(
            transaction.getExtraToken().getFixedCharge() != null ? transaction.getExtraToken().getFixedCharge() : null
        );
        extraData.setLossOfRevenue(
            transaction.getExtraToken().getLossOfRevenue() != null ? transaction.getExtraToken().getLossOfRevenue() : null
        );
        extraData.setPenalty(transaction.getExtraToken().getPenalty() != null ? transaction.getExtraToken().getPenalty() : null);
        extraData.setMeterServiceCharge(
            transaction.getExtraToken().getMeterServiceCharge() != null ? transaction.getExtraToken().getMeterServiceCharge() : null
        );
        extraData.setMeterCost(transaction.getExtraToken().getMeterCost() != null ? transaction.getExtraToken().getMeterCost() : null);
        extraData.setApplicationFee(
            transaction.getExtraToken().getApplicationFee() != null ? transaction.getExtraToken().getApplicationFee() : null
        );
        extraData.setReadingText(
            transaction.getExtraToken().getReadingText() != null ? transaction.getExtraToken().getReadingText() : null
        );
        extraData.setCBTRegistrationCharge(
            transaction.getExtraToken().getCBTRegistrationCharge() != null ? transaction.getExtraToken().getCBTRegistrationCharge() : null
        );
        extraData.setCBTExaminationCharge(
            transaction.getExtraToken().getCBTExaminationCharge() != null ? transaction.getExtraToken().getCBTExaminationCharge() : null
        );
        extraData.setOptionalMock(
            transaction.getExtraToken().getOptionalMock() != null ? transaction.getExtraToken().getOptionalMock() : null
        );
        extraData.setStrisBrilliant(
            transaction.getExtraToken().getStrisBrilliant() != null ? transaction.getExtraToken().getStrisBrilliant() : null
        );
        extraData.setMapAmount(transaction.getExtraToken().getMapAmount() != null ? transaction.getExtraToken().getMapAmount() : null);
        extraData.setRefundAmount(
            transaction.getExtraToken().getRefundAmount() != null ? transaction.getExtraToken().getRefundAmount() : null
        );
        extraData.setRefundUnits(
            transaction.getExtraToken().getRefundUnits() != null ? transaction.getExtraToken().getRefundUnits() : null
        );
        extraData.setAccount(transaction.getExtraToken().getAccount() != null ? transaction.getExtraToken().getAccount() : null);
    }

    public CommissionDTO resolveAmountWithCommission(TransactionRequest request, MerchantDetailsDto merchantDetailsDto, String Processor) {
        log.info("Inside resolveAmountWithCommission with processor...{}", Processor);
        CommissionDTO commissionDTO = new CommissionDTO();
        //check if merchant, product and processor are configured
        Optional<CustomCommission> optionalCustomCommissionWithProcessor = customCommissionRepository.findFirstByMerchantIdAndProductCodeAndProcessor(
            merchantDetailsDto.getOrgId(),
            request.getProductCode(),
            Processor
        );
        if (optionalCustomCommissionWithProcessor.isPresent()) {
            log.info("optionalCustomCommissionWithProcessor ....{}", optionalCustomCommissionWithProcessor);
            CustomCommission commission = optionalCustomCommissionWithProcessor.get();
            if (Boolean.TRUE.equals(commission.getIsAppliedCommission())) {
                if (Boolean.TRUE.equals(commission.getIsFixedCommission())) {
                    commissionDTO.setCommission(commission.getFixedCommission());
                    commissionDTO.setDiscountedAmount(request.getAmount().subtract(commission.getFixedCommission()));
                    return commissionDTO;
                } else {
                    return calculateAmountForPercentageCommission(
                        request.getAmount(),
                        commission.getPercentageCommission(),
                        commission.getPercentageMin(),
                        commission.getPercentageMax()
                    );
                }
            }
        }

        List<CustomCommission> customCommissionList = customCommissionRepository.findByMerchantIdAndProductCode(
                merchantDetailsDto.getOrgId(),
                request.getProductCode()
            ).stream().filter(customCommission -> StringUtils.isBlank(customCommission.getProcessor()))
            .collect(Collectors.toList());
        if (!customCommissionList.isEmpty()) {
            CustomCommission commission = customCommissionList.get(0);
            log.info("Only customCommissionList for product and merchant ....{}", customCommissionList);
            if (Boolean.TRUE.equals(commission.getIsAppliedCommission())) {
                if (Boolean.TRUE.equals(commission.getIsFixedCommission())) {
                    commissionDTO.setCommission(commission.getFixedCommission());
                    commissionDTO.setDiscountedAmount(request.getAmount().subtract(commission.getFixedCommission()));
                    return commissionDTO;
                } else {
                    return calculateAmountForPercentageCommission(
                        request.getAmount(),
                        commission.getPercentageCommission(),
                        commission.getPercentageMin(),
                        commission.getPercentageMax()
                    );
                }
            }
       }
        log.info("Custom Commission not configured");
        Product product = getProduct(request.getProductCode());
        if (Boolean.TRUE.equals(product.getApplyCommission())) {
            if (Boolean.TRUE.equals(product.getIsFixedCommission())) {
                commissionDTO.setCommission(product.getFixedCommission());
                commissionDTO.setDiscountedAmount(request.getAmount().subtract(product.getFixedCommission()));
                return commissionDTO;
            } else {
                return calculateAmountForPercentageCommission(
                    request.getAmount(),
                    product.getPercentageCommission(),
                    product.getPercentageMinCap(),
                    product.getPercentageMaxCap()
                );
            }
        }
        commissionDTO.setCommission(new BigDecimal("0"));
        commissionDTO.setDiscountedAmount(request.getAmount());
        return commissionDTO;
    }

    public CommissionDTO resolveAmountWithPlatformCommission(
        TransactionRequest request,
        MerchantDetailsDto merchantDetailsDto,
        String processor
    ) {
        log.info("Inside resolveAmountWithPlatformCommission...{}", processor);
        CommissionDTO commissionDTO = new CommissionDTO();
        Optional<CustomCommission> optionalCustomCommissionWithProcessor = customCommissionRepository.findFirstByProcessorAndIsPlatformCommissionTrue(
            processor
        );

        if (optionalCustomCommissionWithProcessor.isPresent()) {
            log.info("optionalCustomCommissionWithProcessor ....{}", optionalCustomCommissionWithProcessor);
            CustomCommission commission = optionalCustomCommissionWithProcessor.get();
            if (Boolean.TRUE.equals(commission.getIsAppliedCommission())) {
                if (Boolean.TRUE.equals(commission.getIsFixedCommission())) {
                    commissionDTO.setCommission(commission.getFixedCommission());
                    //commissionDTO.setDiscountedAmount(request.getAmount().subtract(commission.getFixedCommission()));
                    return commissionDTO;
                } else {
                    return calculateAmountForPercentagePlatformCommission(
                        request.getAmount(),
                        commission.getPercentageCommission(),
                        commission.getPercentageMin(),
                        commission.getPercentageMax()
                    );
                }
            }
        }
        return commissionDTO;
    }

    public CommissionDTO calculateAmountForPercentageCommission(
        BigDecimal originalAmount,
        String percentageCommission,
        BigDecimal minAmountCap,
        BigDecimal maxAmountCap
    ) {
        CommissionDTO commissionDTO = new CommissionDTO();
        BigDecimal percentageComm = new BigDecimal(percentageCommission);

        BigDecimal commissionAmount = (originalAmount.multiply(percentageComm)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalCommissionAmount = commissionAmount;
        if (minAmountCap != null) {
            if (commissionAmount.compareTo(minAmountCap) < 0 || commissionAmount.compareTo(minAmountCap) == 0) {
                finalCommissionAmount = minAmountCap;
            }
        }
        if (maxAmountCap != null) {
            if (commissionAmount.compareTo(maxAmountCap) > 0 || commissionAmount.compareTo(maxAmountCap) == 0) {
                finalCommissionAmount = maxAmountCap;
            }
        }
        BigDecimal discountedAmount = originalAmount.subtract(finalCommissionAmount).setScale(2, RoundingMode.HALF_UP);

        commissionDTO.setDiscountedAmount(discountedAmount);
        commissionDTO.setCommission(finalCommissionAmount);
        commissionDTO.setMerchantPercentageCommission(percentageCommission);
        commissionDTO.setMerchantMinAmount(minAmountCap);
        commissionDTO.setMerchantMaxAmount(maxAmountCap);
        return commissionDTO;
    }

    public CommissionDTO calculateAmountForPercentagePlatformCommission(
        BigDecimal originalAmount,
        String percentageCommission,
        BigDecimal minAmountCap,
        BigDecimal maxAmountCap
    ) {
        CommissionDTO commissionDTO = new CommissionDTO();
        BigDecimal percentageComm = new BigDecimal(percentageCommission);

        BigDecimal commissionAmount = (originalAmount.multiply(percentageComm)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalCommissionAmount = commissionAmount;
        if (minAmountCap != null) {
            if (commissionAmount.compareTo(minAmountCap) < 0 || commissionAmount.compareTo(minAmountCap) == 0) {
                finalCommissionAmount = minAmountCap;
            }
        }
        if (maxAmountCap != null) {
            if (commissionAmount.compareTo(maxAmountCap) > 0 || commissionAmount.compareTo(maxAmountCap) == 0) {
                finalCommissionAmount = maxAmountCap;
            }
        }
        //BigDecimal discountedAmount = originalAmount.subtract(finalCommissionAmount).setScale(2, RoundingMode.HALF_UP);

        // commissionDTO.setDiscountedAmount(discountedAmount);
        commissionDTO.setPlatformCommission(finalCommissionAmount);
        commissionDTO.setPlatformPercentageCommission(percentageCommission);
        commissionDTO.setPlatformMinAmount(minAmountCap);
        commissionDTO.setPlatformMaxAmount(maxAmountCap);
        return commissionDTO;
    }

    private BulkQueryResponse mapVendingItemsToBulkQueryResponse(VendingItems vendingItems) {
        BulkQueryResponse bulkQueryResponse = new BulkQueryResponse();
        bulkQueryResponse.setProductCode(vendingItems.getProductCode());
        bulkQueryResponse.setCategoryCode(vendingItems.getCategoryCode());
        bulkQueryResponse.setAccountNumber(vendingItems.getAccountNumber());
        bulkQueryResponse.setPhoneNumber(vendingItems.getPhoneNumber());
        bulkQueryResponse.setAmount(vendingItems.getAmount());
        bulkQueryResponse.setBulkClientReference(vendingItems.getBulkClientReference());
        bulkQueryResponse.setInternalReference(vendingItems.getInternalReference());
        bulkQueryResponse.setVendStatus(vendingItems.getVendStatus());

        return bulkQueryResponse;
    }

    private void checkDuplicateTransaction(TransactionRequest request, MerchantDetailsDto merchantDetailsDto) {
        log.info("Inside checkDuplicateTransaction with reference ...{}", request.getClientReference());
        try {
            DuplicateCheck duplicateCheck = getDuplicateCheck(request, merchantDetailsDto);
            duplicateCheckRepository.save(duplicateCheck);
        } catch (ConstraintViolationException | DataIntegrityViolationException e) {
            e.printStackTrace();
            log.error(">>> Enter because of Duplicate check for " + request.getProductCode() + " " + request.getClientReference());
            throw new BadRequestException(
                TransactionStatus.DUPLICATE_TRANSACTION.getMessage(),
                TransactionStatus.DUPLICATE_TRANSACTION.getCode()
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw new BadRequestException(e.getMessage(), TransactionStatus.DUPLICATE_TRANSACTION.getCode());
        }
    }

    private DuplicateCheck getDuplicateCheck(TransactionRequest request, MerchantDetailsDto merchantDetailsDto) {
        DuplicateCheck duplicateCheck = new DuplicateCheck();
        duplicateCheck.setAmount(request.getAmount());
        duplicateCheck.setProductCode(request.getProductCode());
        duplicateCheck.setClientReference(request.getClientReference());
        duplicateCheck.setCreatedAt(new Date());
        duplicateCheck.setMerchantName(merchantDetailsDto.getRegisteredBusinessName());
        duplicateCheck.setMerchantOrgId(merchantDetailsDto.getOrgId());
        duplicateCheck.setIpAddress(merchantDetailsDto.getRequestIp());

        return duplicateCheck;
    }

    private FlightProductAbstractVendingService getFlightProductVendingService(String processorId) {
        log.info(">>> Getting flightProductVendingService from processorId):{}", processorId);
        FlightProductAbstractVendingService serviceBean = flightProductVendingServiceDelegateBean.getDelegate(processorId);
        if (Objects.isNull(serviceBean)) {
            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }
        return serviceBean;
    }

    private Optional<Transaction> validateAndCheckDuplicate(TransactionRequest request) {
        log.info(">>> Validating and checking duplicate for clientReference: {}", request.getClientReference());

        resolveReference(request);

        Optional<Transaction> existingTransaction = transactionRepository.findByClientReference(request.getClientReference());
        if (existingTransaction.isPresent()) {
            throw new AlreadyExistException("Duplicate transaction detected for paymentIdentifier: " + request.getClientReference());
        }

        return existingTransaction;
    }

    private Optional<Transaction> validateReVendEligibility(TransactionRequest request) {
        log.info(">>> Validating transaction eligibility for re-vend: {}", request.getClientReference());

        resolveReference(request);

        Optional<Transaction> existingTransaction = transactionRepository.findByClientReference(request.getClientReference());

        if (existingTransaction.isPresent()) {
            Transaction transaction = existingTransaction.get();
            log.warn(
                "Transaction exists for re-vend, proceeding to internal reference check for payment identifier: {}",
                request.getClientReference()
            );
            if (transaction.getInternalReference() == null || transaction.getInternalReference().isBlank()) {
                throw new IllegalStateException(
                    "Cannot re-vend as transaction cannot be validated internally. Internal reference missing."
                );
            }
            return existingTransaction;
        }
        return Optional.empty();
    }

    private static class FallbackOutcome {

        final boolean processedWithFallback;
        final String fallbackProcessor;
        final String fallbackFailureMessage;
        final String fallbackResponseCode;
        final String mainFailureMessage;
        final TransactionResponse result;

        FallbackOutcome(
            boolean processedWithFallback,
            String fallbackProcessor,
            String fallbackFailureMessage,
            String fallbackResponseCode,
            String mainFailureMessage,
            TransactionResponse result
        ) {
            this.processedWithFallback = processedWithFallback;
            this.fallbackProcessor = fallbackProcessor;
            this.fallbackFailureMessage = fallbackFailureMessage;
            this.fallbackResponseCode = fallbackResponseCode;
            this.mainFailureMessage = mainFailureMessage;
            this.result = result;
        }

        static FallbackOutcome none(TransactionResponse current) {
            return new FallbackOutcome(false, null, null, null, null, current);
        }
    }
}
