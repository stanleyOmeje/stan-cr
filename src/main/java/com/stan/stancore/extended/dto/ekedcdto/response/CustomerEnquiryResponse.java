package com.stan.stancore.extended.dto.ekedcdto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerEnquiryResponse {

    private String receipt;
    private long idVendor;
    private String codUser;
    private String meterSerial;
    private String account;
    private String customerName;
    private BigDecimal debtPayment;
    private String unitsType;
    private BigDecimal unitsPayment;
    private double units;
    private BigDecimal totalAmount;
    private String descVendor;
    private String dateTransaction;
    private String nameCashier;
    private String tarrif;
    private String transactionId;
    private String printed;
}
