package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.dto.DefaultApiResponse;
import com.systemspecs.remita.dto.account.AccountDebitRequest;
import com.systemspecs.remita.dto.auth.MerchantDetailsDto;
import com.systemspecs.remita.enumeration.AccountType;
import com.systemspecs.remita.enumeration.CoreSDKResult;
import com.systemspecs.remita.sdk.debit.CoreSdkAccount;
import com.systemspecs.remita.vending.extended.config.VendingCoreProperties;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.BadRequestException;
import com.systemspecs.remita.vending.extended.service.BulkVendingService;
import com.systemspecs.remita.vending.extended.util.ReferenceUtil;
import com.systemspecs.remita.vending.extended.util.SubscriptionUtil;
import com.systemspecs.remita.vending.vendingcommon.dto.request.BulkVendingRequest;
import com.systemspecs.remita.vending.vendingcommon.dto.request.VendingItemsRequest;
import com.systemspecs.remita.vending.vendingcommon.entity.BulkVending;
import com.systemspecs.remita.vending.vendingcommon.entity.VendingItems;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import com.systemspecs.remita.vending.vendingcommon.enums.VendStatus;
import com.systemspecs.remita.vending.vendingcommon.repository.BulkVendingRepository;
import com.systemspecs.remita.vending.vendingcommon.repository.VendingItemsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static com.systemspecs.remita.vending.extended.enums.ResponseStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkVendingServiceImpl implements BulkVendingService {

    private final BulkVendingRepository bulkVendingRepository;
    private final VendingItemsRepository vendingItemsRepository;
    private final CoreSdkAccount coreSdkAccount;
    private final VendingCoreProperties properties;

    @Override
    public ResponseEntity<DefaultResponse> processBulkVending(
        BulkVendingRequest bulkVendingRequest,
        MerchantDetailsDto merchantDetailsDto
    ) {
        DefaultResponse response = new DefaultResponse();
        boolean validateAmount = validateAmount(bulkVendingRequest);
        if (!validateAmount) {
            response.setStatus(ResponseStatus.INCORRECT_AMOUNT.getCode());
            response.setMessage(ResponseStatus.INCORRECT_AMOUNT.getMessage());

            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        String subscriptionType = null;
        if (properties.isUseSecretKey()) {
            String profileId = merchantDetailsDto.getOrgId();

            //Get subscriptionType
            subscriptionType = SubscriptionUtil.getSubscriptionType(merchantDetailsDto);
            if (Strings.isBlank(subscriptionType.toString())) {
                throw new BadRequestException("Subscribtion type cannot be blank ", NOT_FOUND.getCode());
            }
        } else {
            subscriptionType = "POSTPAID";
        }

        //if prapaid, debit marchant
        DefaultApiResponse defaultApiResponse = new DefaultApiResponse();
        if (subscriptionType.equalsIgnoreCase("PREPAID")) {
            AccountDebitRequest accountDebitRequest = new AccountDebitRequest();
            if (merchantDetailsDto == null) {
                throw new BadRequestException("Merchant not found", NOT_FOUND.getCode());
            }
            accountDebitRequest.setAccountNumber(merchantDetailsDto.getAccountNumber());
            accountDebitRequest.setBankCode(merchantDetailsDto.getBankCode());
            accountDebitRequest.setAccountType(AccountType.PREPAID);
            accountDebitRequest.setAmount(bulkVendingRequest.getTotalAmount());
            accountDebitRequest.setDebitRef(bulkVendingRequest.getClientReference());
            accountDebitRequest.setServiceName("remita-wallet");
            accountDebitRequest.setOrgId(merchantDetailsDto.getOrgId());

            CoreSDKResult debitResult = coreSdkAccount.debit(accountDebitRequest);
            if (debitResult == CoreSDKResult.FAILED) {
                response.setStatus("99");
                response.setMessage("Debit Unsuccessful");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            log.info(
                "Merchant with profileId {}, and account number {}, debited successfully by the amount: {} ",
                merchantDetailsDto.getOrgId(),
                merchantDetailsDto.getAccountNumber(),
                bulkVendingRequest.getTotalAmount()
            );
        }

        Optional<BulkVending> optionalBulkVending = bulkVendingRepository.findByClientReference(bulkVendingRequest.getClientReference());
        if (optionalBulkVending.isPresent()) {
            log.info("BulkVending already exist for rrr {} ", bulkVendingRequest.getClientReference());
            response.setStatus(ResponseStatus.ALREADY_EXIST.getCode());
            response.setMessage(ResponseStatus.ALREADY_EXIST.getMessage());
        } else {
            try {
                BulkVending bulkVending = mapBulkVendingRequestToBulkVending(bulkVendingRequest);
                log.info("BulkVending request saved {} ", bulkVendingRequest.getClientReference());
                bulkVendingRepository.save(bulkVending);
                List<VendingItems> vendingItems = mapVendingItemRequestToVendingItem(
                    bulkVendingRequest.getItems(),
                    bulkVendingRequest.getClientReference(),
                    merchantDetailsDto
                );
                vendingItemsRepository.saveAll(vendingItems);
                response.setStatus(ResponseStatus.SUCCESS.getCode());
                response.setMessage("BulkVending Request successfully collected. Queued for processing");
                HashMap<String, Object> mapData = new HashMap<>();
                mapData.put("ClientReference", bulkVendingRequest.getClientReference());
                response.setData(mapData);
            } catch (Exception e) {
                log.error("Error saving bulk vending request {} ", e);
                response.setStatus(ResponseStatus.INTERNAL_SERVER_ERROR.getCode());
                response.setMessage(ResponseStatus.INTERNAL_SERVER_ERROR.getMessage());
            }
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private BulkVending mapBulkVendingRequestToBulkVending(BulkVendingRequest bulkVendingRequest) {
        return BulkVending
            .builder()
            .clientReference(bulkVendingRequest.getClientReference())
            .totalAmount(bulkVendingRequest.getTotalAmount())
            .profileId(bulkVendingRequest.getProfileId())
            .vendStatus(VendStatus.PENDING)
            .build();
    }

    private List<VendingItems> mapVendingItemRequestToVendingItem(
        List<VendingItemsRequest> items,
        String clientReference,
        MerchantDetailsDto merchantDetailsDto
    ) {
        List<VendingItems> vendingItemsList = new ArrayList<>();

        for (VendingItemsRequest item : items) {
            VendingItems vendingItem = VendingItems
                .builder()
                .productCode(item.getProductCode())
                .categoryCode(item.getCategoryCode())
                .accountNumber(item.getAccountNumber())
                .amount(item.getAmount())
                .vendStatus(TransactionStatus.PENDING)
                .isBulkVending(true)
                .bulkClientReference(clientReference)
                .phoneNumber(item.getPhoneNumber())
                .internalReference(ReferenceUtil.generateInternalReference())
                .merchantAccountNumber(merchantDetailsDto.getAccountNumber())
                .merchantId(merchantDetailsDto.getTenantId())
                .merchantName(merchantDetailsDto.getRegisteredBusinessName())
                .bankCode(merchantDetailsDto.getBankCode())
                .ipAddress(merchantDetailsDto.getRequestIp())
                .orgId(merchantDetailsDto.getOrgId())
                .build();

            vendingItemsList.add(vendingItem);
        }

        return vendingItemsList;
    }

    private boolean validateAmount(BulkVendingRequest bulkVendingRequest) {
        log.info("Validating amount for bulk vending request {} ", bulkVendingRequest.getClientReference());

        if (bulkVendingRequest.getItems() != null && !bulkVendingRequest.getItems().isEmpty()) {
            BigDecimal amount = bulkVendingRequest
                .getItems()
                .stream()
                .map(VendingItemsRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            return (
                (bulkVendingRequest.getTotalAmount().compareTo(amount) == 0) || bulkVendingRequest.getTotalAmount().compareTo(amount) > 0
            );
        } else {
            throw new IllegalArgumentException("Items cannot be empty");
        }
    }
}
