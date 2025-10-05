package com.stan.stancore.extended.dto.response;

import lombok.Data;

@Data
public class FundRecoupErrorMap {
    private String reference;
    private String errorMessage;
}
