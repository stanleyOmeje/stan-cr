package com.stan.stancore.extended.dto.ekedcdto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.systemspecs.remita.vending.ekedcmodule.dto.response.UnitsTopUp;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CalculatePaymentResponse {

    private String status;
    private String message;

    private String code;
    private String msgUser;
    private String msgDeveloper;
    public String account;
    public BigDecimal accountBalance;
    public double amountLast;
    public String comment;
    public String customerName;
    public BigDecimal debtPayment;
    public long lastPaymentDate;
    public String meterSerial;
    public double percentageDebt;
    public String serviceAddress;
    public String tariffDescription;
    public double units;
    public BigDecimal unitsPayment;
    public ArrayList<UnitsTopUp> unitsTopUp;
    public String unitsType;
}
