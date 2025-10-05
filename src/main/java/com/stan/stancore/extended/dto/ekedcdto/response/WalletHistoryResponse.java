package com.stan.stancore.extended.dto.ekedcdto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WalletHistoryResponse {

    @JsonProperty("merchantcode")
    private String merchantCode;

    private boolean active;
    private String paymentDate;
    private String amountPaid;
}
