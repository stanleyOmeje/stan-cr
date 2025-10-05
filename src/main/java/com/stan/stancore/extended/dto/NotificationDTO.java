package com.stan.stancore.extended.dto;

import lombok.Data;

@Data
public class NotificationDTO {

    private String callBackUrl;
    private NotificationData data;
}
