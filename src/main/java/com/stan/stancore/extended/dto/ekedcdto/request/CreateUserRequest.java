package com.stan.stancore.extended.dto.ekedcdto.request;

import lombok.Data;

@Data
public class CreateUserRequest {

    private long idVendor;
    private String codUser;
    private String name;
    private String codNewUser;
    private String email;
    private boolean userAdmin;
    private boolean active;
    private String phoneNo;
}
