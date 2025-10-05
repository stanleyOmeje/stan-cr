package com.stan.stancore.extended.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class LocalEventResponse {

    private String bookingId;
    private int ticketQuantity;
    private BigDecimal ticketAmount;
    private String bookingRef;
    private Date createdAt;
    private Date expiresAt;
}
