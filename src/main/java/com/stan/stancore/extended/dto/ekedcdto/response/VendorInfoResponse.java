package com.stan.stancore.extended.dto.ekedcdto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class VendorInfoResponse {

    private String idVendor;
    private BigDecimal balance;
    private String descVendor;
    private String district;
    private int displayBalance;
    private int lowBalanceAlert;
}
