package com.stan.stancore.extended.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExtraTokenNotification {

    private String kct1;
    private String kct2;
    private String bsstTokenValue;
    private String bsstTokenUnits;
    private String standardTokenValue;
    private String standardTokenUnits;
    private String pin;
    private String meterNumber;
    private String customerName;
    private String receiptNumber;
    private String tariffClass;
    private BigDecimal amountPaid;
    private BigDecimal costOfUnit;
    private BigDecimal amountForDebt;
    private String unitsType;
    private BigDecimal accountBalance;
    private String mapToken1;
    private String mapToken2;
    private String mapUnits;
    private String tariffRate;
    private String address;
    private BigDecimal vat;
    private String message;
    private String unitsPurchased;
    private String accountType;
    private BigDecimal minVendAmount;
    private BigDecimal maxVendAmount;
    private BigDecimal remainingDebt;
    private String meterType;
    private BigDecimal replacementCost;
    private BigDecimal outstandingDebt;
    private BigDecimal administrativeCharge;
    private BigDecimal fixedCharge;
    private BigDecimal lossOfRevenue;
    private BigDecimal penalty;
    private BigDecimal meterServiceCharge;
    private BigDecimal meterCost;
    private String token;
    private String units;
}
