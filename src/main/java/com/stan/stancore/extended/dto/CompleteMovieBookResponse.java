package com.stan.stancore.extended.dto;

import com.systemspecs.remita.vending.vendingcommon.movies.dto.response.CompleteMovieBookingResult;
import lombok.Data;

@Data
public class CompleteMovieBookResponse {

    private String fallbackProcessor;
    private CompleteMovieBookingResult fallbackResult;
    private String fallbackFailureMessage;
    private String mainFailureMessage;
    private boolean processedWithFallback;
    private String fallbackResponseCode;
    private String transactionStatus;
    private CompleteMovieBookingResult result;
}
