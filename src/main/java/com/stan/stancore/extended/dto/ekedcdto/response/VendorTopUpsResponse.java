package com.stan.stancore.extended.dto.ekedcdto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class VendorTopUpsResponse {

       private BigDecimal balance;
        private long idVendor;
        private String codUser;
        private List<TopUpData> topUpData;
}
