package com.stan.stancore.extended.dto.ekedcdto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CalculatePaymentRequest {

    private String idVendor;
    private String codUser;
    private String meterSerial;
    private Long account;
    private BigDecimal totalPayment;
    private BigDecimal debtPayment;
}
