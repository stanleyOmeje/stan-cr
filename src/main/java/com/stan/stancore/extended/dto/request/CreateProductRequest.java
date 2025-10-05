package com.stan.stancore.extended.dto.request;

import com.systemspecs.remita.vending.vendingcommon.enums.FeeType;
import com.systemspecs.remita.vending.vendingcommon.enums.ProductType;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class CreateProductRequest {

    @NotBlank
    private String code;

    private String name;

    private String description;

    @NotBlank
    private String categoryCode;

    @NotNull(message = "fee type is required")
    private FeeType feeType;

    private BigDecimal amount;

    private String country;

    private String countryCode;

    private String currencyCode;

    private String calculationMode;

    private ProductType productType;

    private Boolean applyCommission = false;

    private BigDecimal fixedCommission;

    private String percentageCommission;

    private Boolean isFixedCommission;
    private BigDecimal percentageMaxCap;
    private BigDecimal percentageMinCap;

    private String provider;
    private Boolean active;

    public CreateProductRequest() {}
}
