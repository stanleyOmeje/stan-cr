package com.stan.stancore.extended.dto.ekedcdto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class SearchUserResponse {

    private String codUser;
    private String name;
    private String email;
    private int active;
    private int userAdmin;
    private String phoneNo;
}
