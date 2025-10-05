package com.stan.stancore.extended.dto.ekedcdto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SearchCustomerRequest {

    private String idVendor;
    private String codUser;
    private String codType;
    private String value;
    private BigDecimal totalPayment;
}
