package com.stan.stancore.extended.dto;

import com.systemspecs.remita.vending.vendingcommon.eventsticketing.dto.response.CompleteEventBookingResult;
import lombok.Data;

@Data
public class CompleteBookResponse {

    private String fallbackProcessor;
    private CompleteEventBookingResult fallbackResult;
    private String fallbackFailureMessage;
    private String mainFailureMessage;
    private boolean processedWithFallback;
    private String fallbackResponseCode;
    private String transactionStatus;
    private CompleteEventBookingResult result;
}
