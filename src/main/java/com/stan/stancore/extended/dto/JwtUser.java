package com.stan.stancore.extended.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.systemspecs.remita.vending.extended.dto.Organization;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JwtUser implements Serializable {

    private Long id;
    private String login;
    private String firstName;
    private String lastName;
    private String otherName;
    private String primaryOrganization;
    private String alias;
    private String email;
    private String phone;
    private boolean enabledTwoFa;
    private String profileType;
    private String lastLogin;
    private boolean requiresPasswordChange;
    private boolean requiresTwoFaChange;
    private String phoneCountryCode;
    private String imageUrl;
    private String langKey;
    private boolean activated;
    private String status;
    ArrayList<Organization> assignedOrgs;
}
