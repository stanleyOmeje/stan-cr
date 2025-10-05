package com.stan.stancore.extended.dto.ekedcdto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TopUpData {
    private long cancelled;
    private long processed;
    private String receiptNo;
    private String requestID;
    private BigDecimal transactionAmount;
    private long  transactionDate;
    private String  transactionType;
}
