package com.stan.stancore.extended.dto.request;

import lombok.Data;

@Data
public class UpdateMerchantNotificationConfigRequest {

    private Boolean enableSms;
    private Boolean enableEmail;
    private String notificatonUrl;
}
