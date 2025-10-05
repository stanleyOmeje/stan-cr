package com.stan.stancore.extended.dto.ekedcdto.request;

import lombok.Data;

@Data
public class ChangePasswordRequest {

    private long idVendor;
    private String codUser;
    private String currentPwd;
    private String newPwd;
}
