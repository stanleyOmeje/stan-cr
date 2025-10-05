package com.stan.stancore.extended.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CommissionDTO {

    private BigDecimal commission;
    private BigDecimal discountedAmount;
    private String merchantPercentageCommission;
    private BigDecimal merchantMinAmount;
    private BigDecimal merchantMaxAmount;
    private String platformPercentageCommission;
    private BigDecimal platformMinAmount;
    private BigDecimal platformMaxAmount;
    private BigDecimal platformCommission;

    public CommissionDTO() {}

    public CommissionDTO(BigDecimal commission, BigDecimal discountedAmount) {
        this.commission = commission;
        this.discountedAmount = discountedAmount;
    }
}
