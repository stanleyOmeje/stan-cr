package com.stan.stancore.extended.util;

import com.systemspecs.remita.dto.auth.MerchantDetailsDto;
import com.systemspecs.remita.dto.auth.ReturnedSubscribedService;
import com.systemspecs.remita.enumeration.ServicesEnum;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

@Slf4j
public class SubscriptionUtil {

    public static String getSubscriptionType(MerchantDetailsDto merchantDetailsDto) {
        log.info("MerchantDetailsDto inside getSubscriptionType...{}", merchantDetailsDto);
        ReturnedSubscribedService returnedSubscribedService = new ReturnedSubscribedService();
        List<ReturnedSubscribedService> subscribedServices = merchantDetailsDto.getSubscriptionService();
        log.info("ReturnedSubscribedService inside getSubscriptionType...{}", subscribedServices);
        if (subscribedServices.isEmpty()) {
            throw new NotFoundException("Merchant not subscribed to vending");
        }
        returnedSubscribedService =
            subscribedServices
                .stream()
                .filter(service -> {
                    return (
                        StringUtils.isNotBlank(service.getServiceName()) &&
                        service.getServiceName().equalsIgnoreCase(ServicesEnum.VENDING.name())
                    );
                })
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Merchant does not subscribe to Vending service"));
        log.info(
            "SubscriptionType from returnedSubscribedService inside getSubscriptionType...{}",
            returnedSubscribedService.getBillingType()
        );

        return returnedSubscribedService.getBillingType() != null ? returnedSubscribedService.getBillingType() : "PREPAID";
    }

    public static boolean getAutoReversalEnabled(MerchantDetailsDto merchantDetailsDto) {
        boolean autoReversalEnabled = false;
        ReturnedSubscribedService returnedSubscribedService = new ReturnedSubscribedService();
        List<ReturnedSubscribedService> subscribedServices = merchantDetailsDto.getSubscriptionService();
        returnedSubscribedService =
            subscribedServices
                .stream()
                .filter(service -> {
                    return (
                        StringUtils.isNotBlank(service.getServiceName()) &&
                        service.getServiceName().equalsIgnoreCase(ServicesEnum.VENDING.name())
                    );
                })
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Merchant does not subscribe to Vending service"));
        if (Objects.nonNull(returnedSubscribedService)) {
            autoReversalEnabled = returnedSubscribedService.isAutoReversalEnabled();
        }

        return autoReversalEnabled;
    }

    public static boolean getRequiresServiceAccount(MerchantDetailsDto merchantDetailsDto) {
        log.info("Inside getRequiresServiceAccount with MerchantDetailsDto ...{}", merchantDetailsDto);
        boolean requireServiceAccount = false;
        ReturnedSubscribedService returnedSubscribedService = new ReturnedSubscribedService();
        List<ReturnedSubscribedService> subscribedServices = merchantDetailsDto.getSubscriptionService();
        returnedSubscribedService =
            subscribedServices
                .stream()
                .filter(service -> {
                    return (
                        StringUtils.isNotBlank(service.getServiceName()) &&
                        service.getServiceName().equalsIgnoreCase(ServicesEnum.VENDING.name())
                    );
                })
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Merchant does not subscribe to Vending service"));
        if (Objects.nonNull(returnedSubscribedService)) {
            requireServiceAccount = returnedSubscribedService.getRequiresServiceAccount();
        }

        return requireServiceAccount;
    }
}
