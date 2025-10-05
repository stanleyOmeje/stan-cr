package com.stan.stancore.extended.dto.ekedcdto.request;

import lombok.Data;

@Data
public class ValidatePasswordRequest {

    private long idVendor;
    private String codUser;
    private String currentPwd;
}
