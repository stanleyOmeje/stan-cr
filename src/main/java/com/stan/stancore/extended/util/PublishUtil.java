package com.stan.stancore.extended.util;

import com.systemspecs.remita.vending.extended.config.VendingCoreProperties;
import com.systemspecs.remita.vending.extended.dto.ExtraTokenNotification;
import com.systemspecs.remita.vending.extended.dto.NotificationDTO;
import com.systemspecs.remita.vending.extended.dto.NotificationData;
import com.systemspecs.remita.vending.extended.event.publishers.service.TransactionNotificationEventPublisher;
import com.systemspecs.remita.vending.vendingcommon.entity.ExtraToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PublishUtil {

    @Autowired
    private TransactionNotificationEventPublisher transactionNotificationEventPublisher;

    @Autowired
    private VendingCoreProperties vendingCoreProperties;

    public void publishVendNotification(ExtraToken extraToken, String TransactionRef) {
        log.info("[+]inside PublishUtil.publishVendNotification...");
        NotificationDTO notificationDTO = buildNotificationDTO(extraToken, TransactionRef);
        log.info("Vending Notification DTO: {}", notificationDTO);
        transactionNotificationEventPublisher.publishEvent(notificationDTO);
    }

    private NotificationDTO buildNotificationDTO(ExtraToken extraToken, String clientReference) {
        String token = extraToken.getStandardTokenValue();
        NotificationDTO notificationDTO = new NotificationDTO();
        notificationDTO.setCallBackUrl(vendingCoreProperties.getCallBackUrl());
        NotificationData data = new NotificationData();

        ExtraTokenNotification extraTokenNotification = new ExtraTokenNotification();
        extraTokenNotification.setToken(token);
        extraTokenNotification.setStandardTokenValue(token);
        extraTokenNotification.setKct1(extraToken.getKct1());
        extraTokenNotification.setKct2(extraToken.getKct2());
        extraTokenNotification.setBsstTokenValue(extraToken.getBsstTokenValue());
        extraTokenNotification.setBsstTokenUnits(extraToken.getBsstTokenUnits());
        extraTokenNotification.setStandardTokenUnits(extraToken.getStandardTokenUnits());
        extraTokenNotification.setPin(extraToken.getPin());
        extraTokenNotification.setMeterNumber(extraToken.getMeterNumber());
        extraTokenNotification.setCustomerName(extraToken.getCustomerName());
        extraTokenNotification.setReceiptNumber(extraToken.getReceiptNumber());
        extraTokenNotification.setTariffClass(extraToken.getTariffClass());
        extraTokenNotification.setAmountPaid(extraToken.getAmountPaid());
        extraTokenNotification.setCostOfUnit(extraToken.getCostOfUnit());
        extraTokenNotification.setAmountForDebt(extraToken.getAmountForDebt());
        extraTokenNotification.setUnitsType(extraToken.getUnitsType());
        extraTokenNotification.setAccountBalance(extraToken.getAccountBalance());
        extraTokenNotification.setMapToken1(extraToken.getMapToken1());
        extraTokenNotification.setMapToken2(extraToken.getMapToken2());
        extraTokenNotification.setMapUnits(extraToken.getMapUnits());
        extraTokenNotification.setTariffRate(extraToken.getTariffRate());
        extraTokenNotification.setAddress(extraToken.getAddress());
        extraTokenNotification.setVat(extraToken.getVat());
        extraTokenNotification.setMessage(extraToken.getMessage());
        extraTokenNotification.setUnitsPurchased(extraToken.getUnitsPurchased());

        extraTokenNotification.setAccountType(extraToken.getAccountType());
        extraTokenNotification.setMinVendAmount(extraToken.getMinVendAmount());
        extraTokenNotification.setMaxVendAmount(extraToken.getMaxVendAmount());
        extraTokenNotification.setRemainingDebt(extraToken.getRemainingDebt());
        extraTokenNotification.setMeterType(extraToken.getMeterType());
        extraTokenNotification.setReplacementCost(extraToken.getReplacementCost());
        extraTokenNotification.setOutstandingDebt(extraToken.getOutstandingDebt());
        extraTokenNotification.setAdministrativeCharge(extraToken.getAdministrativeCharge());
        extraTokenNotification.setFixedCharge(extraToken.getFixedCharge());

        extraTokenNotification.setLossOfRevenue(extraToken.getLossOfRevenue());
        extraTokenNotification.setPenalty(extraToken.getPenalty());
        extraTokenNotification.setMeterServiceCharge(extraToken.getMeterServiceCharge());
        extraTokenNotification.setMeterCost(extraToken.getMeterCost());
        extraTokenNotification.setUnits(extraToken.getUnitsType());

        data.setTransactionReference(clientReference);
        data.setExtraToken(extraTokenNotification);
        notificationDTO.setData(data);
        return notificationDTO;
    }
}
