package com.stan.stancore.extended.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class LookUpOperatorsRequest {
    @JsonProperty("mobile_number")
    private String mobileNumber;
    private int page;
    @JsonProperty("per_page")
    private int perPage;
}
