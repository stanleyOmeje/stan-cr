package com.stan.stancore.extended.dto.ekedcdto.request;

import lombok.Data;

@Data
public class VendorTopUpsRequest {

    private String idVendor;
    private String codUser;
    private long dateFrom;
    private long dateTo;
}
