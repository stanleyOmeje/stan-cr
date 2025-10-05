package com.stan.stancore.extended.dto;

import lombok.Data;

@Data
public class NotificationData {

    private String transactionReference;
    private ExtraTokenNotification extraToken;
}
