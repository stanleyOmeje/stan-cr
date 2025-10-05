package com.stan.stancore.extended.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Data
public class ValidationRequest {

    @JsonProperty("accountNumber")
    @NotBlank(message = "unique identifier is required")
    private String uniqueIdentifier;

    @NotBlank(message = "product code is required")
    private String productCode;

    private BigDecimal amount;

    public ValidationRequest() {}

    public String getUniqueIdentifier() {
        return uniqueIdentifier;
    }

    public void setUniqueIdentifier(String uniqueIdentifier) {
        this.uniqueIdentifier = uniqueIdentifier;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }
}
