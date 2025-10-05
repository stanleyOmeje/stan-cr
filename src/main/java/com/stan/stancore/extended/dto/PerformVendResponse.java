package com.stan.stancore.extended.dto;

import com.systemspecs.remita.vending.vendingcommon.dto.response.TransactionResponse;
import lombok.Data;

@Data
public class PerformVendResponse {

    private String fallbackProcessor;
    private TransactionResponse fallbackResult;
    private String fallbackFailureMessage;
    private String mainFailureMessage;
    private boolean processedWithFallback;
    private String fallbackResponseCode;
    private String transactionStatus;
    private TransactionResponse result;
}
