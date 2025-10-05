package com.stan.stancore.extended.dto.ekedcdto.request;

import lombok.Data;

@Data
public class ShiftEnquiryRequest {

    private String idVendor;
    private String codUser;
    private String codUserShift;
    private long paymentDate;
    private boolean paginate = false;
}
