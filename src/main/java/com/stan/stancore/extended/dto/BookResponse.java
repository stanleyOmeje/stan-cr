package com.stan.stancore.extended.dto;

import com.systemspecs.remita.vending.vendingcommon.flight.dto.response.BookingResult;
import lombok.Data;

@Data
public class BookResponse {

    private String fallbackProcessor;
    private BookingResult fallbackResult;
    private String fallbackFailureMessage;
    private String mainFailureMessage;
    private boolean processedWithFallback;
    private String fallbackResponseCode;
    private String transactionStatus;
    private BookingResult result;
}
