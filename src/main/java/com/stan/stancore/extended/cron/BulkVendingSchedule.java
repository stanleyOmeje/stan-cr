package com.stan.stancore.extended.cron;

import com.systemspecs.remita.dto.auth.MerchantDetailsDto;
import com.systemspecs.remita.vending.extended.config.VendingCoreProperties;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.service.impl.TransactionServiceImp;
import com.systemspecs.remita.vending.extended.util.VendAmountUtil;
import com.systemspecs.remita.vending.vendingcommon.dto.request.TransactionDataRequest;
import com.systemspecs.remita.vending.vendingcommon.dto.request.TransactionRequest;
import com.systemspecs.remita.vending.vendingcommon.dto.response.LocalTransactionResponseForBulkVend;
import com.systemspecs.remita.vending.vendingcommon.entity.BulkVending;
import com.systemspecs.remita.vending.vendingcommon.entity.VendingItems;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import com.systemspecs.remita.vending.vendingcommon.enums.VendStatus;
import com.systemspecs.remita.vending.vendingcommon.repository.BulkVendingRepository;
import com.systemspecs.remita.vending.vendingcommon.repository.VendingItemsRepository;
import com.systemspecs.remita.vending.vendingcommon.util.PhoneUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class BulkVendingSchedule {

    private final VendingItemsRepository vendingItemsRepository;
    private final BulkVendingRepository bulkVendingRepository;
    private final TransactionServiceImp transactionServiceImp;
    private static final String ELECTRICITY = "electricity";
    private static final String AIRTIME = "airtime";
    private final VendingCoreProperties properties;

    @Scheduled(cron = "${vending.cron.duration: * * * * * *}")
    public void processBulkVending() {
        int pageNumber = 0;
        while (properties.isEnabledBulkVendCron()) {
            PageRequest page = PageRequest.of(pageNumber, properties.getPageSize());
            List<VendingItems> vendingItemsList = vendingItemsRepository.findAllByVendStatus(TransactionStatus.PENDING, page);

            if (vendingItemsList.isEmpty()) {
                break;
            }
            vendingItemsList.forEach(item -> {
                Optional<BulkVending> bulKVend = bulkVendingRepository.findByClientReference(item.getBulkClientReference());
                VendStatus vendStatus = bulKVend.get().getVendStatus();
                boolean itemValid = validateItems(item);
                if (itemValid) {
                    item.setValidationStatus(VendStatus.SUCCESSFUL_VALIDATION);
                    TransactionRequest transactionRequest = mapVendingItemToTransactionRequest(item, bulKVend.get().getProfileId());
                    MerchantDetailsDto merchantDetailsDto = getMerchantDetailsDto(item);
                    ResponseEntity<DefaultResponse> response = transactionServiceImp.performTransaction(
                        transactionRequest,
                        merchantDetailsDto
                    );
                    if (response != null && response.getStatusCode().equals(HttpStatus.OK)) {
                        LocalTransactionResponseForBulkVend finalVendResponse = (LocalTransactionResponseForBulkVend) response
                            .getBody()
                            .getData();
                        item.setVendStatus(finalVendResponse.getStatus());
                        setBulkVendStatus(bulKVend, vendStatus);
                    } else {
                        item.setVendStatus(TransactionStatus.TRANSACTION_FAILED);
                        setBulkVendStatus(bulKVend, vendStatus);
                    }
                } else {
                    item.setValidationStatus(VendStatus.FAILED_VALIDATION);
                    item.setVendStatus(TransactionStatus.TRANSACTION_FAILED);
                    setBulkVendStatus(bulKVend, vendStatus);
                }
                vendingItemsRepository.save(item);
            });
            pageNumber++;
        }
    }

    private static MerchantDetailsDto getMerchantDetailsDto(VendingItems vendingItems) {
        MerchantDetailsDto merchantDetailsDto = new MerchantDetailsDto();
        merchantDetailsDto.setOrgId(vendingItems.getOrgId());
        merchantDetailsDto.setRegisteredBusinessName(vendingItems.getMerchantName());
        merchantDetailsDto.setTenantId(vendingItems.getMerchantId());
        merchantDetailsDto.setRequestIp(vendingItems.getIpAddress());
        merchantDetailsDto.setAccountNumber(vendingItems.getAccountNumber());
        merchantDetailsDto.setBankCode(vendingItems.getBankCode());
        return merchantDetailsDto;
    }

    private void setBulkVendStatus(Optional<BulkVending> bulKVend, VendStatus vendStatus) {
        if (vendStatus.equals(VendStatus.PENDING)) {
            bulKVend.get().setVendStatus(VendStatus.SUCCESSFUL_PROCESSING);
            bulKVend.get().setProcessed(true);
            bulkVendingRepository.save(bulKVend.get());
        }
    }

    private TransactionRequest mapVendingItemToTransactionRequest(VendingItems vendingItems, String profileId) {
        TransactionRequest request = new TransactionRequest();

        request.setAmount(vendingItems.getAmount());
        request.setProductCode(vendingItems.getProductCode());
        request.setClientReference(vendingItems.getBulkClientReference());
        request.setProfileId(profileId);
        request.setInternalReference(vendingItems.getInternalReference());
        request.setBulkVending(true);

        TransactionDataRequest dataRequest = new TransactionDataRequest();
        dataRequest.setAccountNumber(vendingItems.getAccountNumber());
        dataRequest.setPhoneNumber(vendingItems.getPhoneNumber());

        request.setData(dataRequest);

        return request;
    }

    private boolean validateItems(VendingItems vendingItems) {
        if (ELECTRICITY.equalsIgnoreCase(vendingItems.getCategoryCode())) {
            if (!PhoneUtil.validNigeriaPhoneNumber(vendingItems.getPhoneNumber())) {
                return false;
            }
            if (!VendAmountUtil.validMinAmount(vendingItems.getAmount())) {
                return false;
            }
        }
        if (AIRTIME.equalsIgnoreCase(vendingItems.getCategoryCode())) {
            return PhoneUtil.validNigeriaPhoneNumber(vendingItems.getPhoneNumber());
        }
        return true;
    }
}
