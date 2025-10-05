package com.stan.stancore.extended.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Organization implements Serializable {

    private Long id;

    @NotNull
    private String name;

    @NotNull
    private String code;

    @NotNull
    private Boolean active;

    private Boolean complianceDone;

    @NotNull
    private String orgType;

    private String logoUrl;

    private String environment;

    private String language;

    private String product;

    private Organization relatedOrgs;

}
