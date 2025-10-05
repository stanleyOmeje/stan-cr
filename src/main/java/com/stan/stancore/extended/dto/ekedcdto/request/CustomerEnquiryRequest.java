package com.stan.stancore.extended.dto.ekedcdto.request;

import lombok.Data;

@Data
public class CustomerEnquiryRequest {

    private long idVendor;
    private String codUser;
    private String receipt;
    private String meterSerial;
    private String account;
    private long dateFrom;
    private long dateTo;
}
