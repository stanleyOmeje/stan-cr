package com.stan.stancore.extended.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.systemspecs.remita.vending.vendingcommon.enums.FeeType;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.math.BigDecimal;
import java.util.Date;

@Data
@RequiredArgsConstructor
public class FeeMappingRequest {
    @Enumerated(EnumType.STRING)
    private FeeType feeType;

    private BigDecimal amount;

    @Column(unique = true)
    private String productCode;

    @JsonIgnore
    private Date createdAt;

    @JsonIgnore
    private Date updatedAt;
}
