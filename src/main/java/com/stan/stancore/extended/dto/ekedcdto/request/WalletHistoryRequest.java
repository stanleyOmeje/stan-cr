package com.stan.stancore.extended.dto.ekedcdto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class WalletHistoryRequest {

    private String merchantId;
    private String token;
    private LocalDate start;
    private LocalDate end;
}
