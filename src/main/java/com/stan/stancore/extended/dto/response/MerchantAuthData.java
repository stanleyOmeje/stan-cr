package com.stan.stancore.extended.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantAuthData {
    private String bankCode;
    private String billingType;
    private String registeredBusinessName;
    private String accountNumber;
    private String webhookUrl;
}
