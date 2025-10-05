package com.stan.stancore.extended.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.systemspecs.remita.vending.vendingcommon.entity.Category;
import com.systemspecs.remita.vending.vendingcommon.enums.FeeType;
import com.systemspecs.remita.vending.vendingcommon.enums.ProductType;
import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class DisplayProduct {

    private String name;
    private String description;
    private String code;

    @JsonIgnore
    @ManyToOne
    private Category category;

    @JsonIgnore
    private Date createdAt;

    @JsonIgnore
    private Date updatedAt;

    @JsonProperty("categoryCode")
    public String categoryCode() {
        return category == null ? null : category.getCode();
    }

    private String country = "Nigeria";

    private String countryCode = "NGA";

    private String currencyCode = "NGN";

    @Enumerated(EnumType.STRING)
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    private FeeType feeType;

    private BigDecimal amount;

    private String provider;
}
