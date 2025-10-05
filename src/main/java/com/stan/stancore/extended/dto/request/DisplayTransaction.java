package com.stan.stancore.extended.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class DisplayTransaction {

    private BigDecimal amount;
    private String merchantOrgId;
    private String productCode;
    private String categoryCode;
    private String clientReference;
    private String status;
    private String transactionDate;
    private String merchantName;
    private String subscriptionType;
}
