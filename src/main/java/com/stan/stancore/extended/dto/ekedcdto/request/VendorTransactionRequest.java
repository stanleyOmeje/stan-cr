package com.stan.stancore.extended.dto.ekedcdto.request;

import lombok.Data;

@Data
public class VendorTransactionRequest {

    private String idVendor;
    private String password;
    private long dateFrom;
    private long dateTo;
}
