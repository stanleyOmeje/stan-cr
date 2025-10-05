package com.stan.stancore.extended.dto.ekedcdto.response;

import lombok.Data;

@Data
public class ValidatePasswordResponse {

    private long idVendor;
    private String descVendor;
    private String codUser;
    private boolean firstConnection;
    private boolean userAdmin;
}
