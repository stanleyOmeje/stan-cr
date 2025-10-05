package com.stan.stancore.extended.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JWTPojo implements Serializable {

    private String iss;
    private float iat;
    private String sub;
    private String auth;
    JwtUser user;
    private String ttype;
    private String orgId;
    private float exp;
}
