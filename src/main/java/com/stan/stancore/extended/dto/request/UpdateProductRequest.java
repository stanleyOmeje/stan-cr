package com.stan.stancore.extended.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Data
public class UpdateProductRequest {

    @NotBlank
    private String name;

    private String description;

    @NotBlank
    private String categoryCode;

    private BigDecimal amount;

    private Boolean applyCommission = false;

    private BigDecimal fixedCommission;

    private String percentageCommission;

    private Boolean isFixedCommission;
    private BigDecimal percentageMaxCap;
    private BigDecimal percentageMinCap;
    private String provider;
    private Boolean active;

    public UpdateProductRequest() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }
}
