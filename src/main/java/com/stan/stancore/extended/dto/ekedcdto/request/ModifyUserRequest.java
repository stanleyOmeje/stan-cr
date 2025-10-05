package com.stan.stancore.extended.dto.ekedcdto.request;

import lombok.Data;

@Data
public class ModifyUserRequest {

    private long idVendor;
    private String codUser;
    private String codUserMod;
    private String newName;
    private String newEmail;
    private int userAdmin;
    private int newIsActive;
}
