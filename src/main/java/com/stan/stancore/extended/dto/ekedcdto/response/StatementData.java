package com.stan.stancore.extended.dto.ekedcdto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StatementData {
    private long transactionDate;

    private String transactionType;
    private BigDecimal openingBalance;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal closingBalance;
}
