package com.stan.stancore.extended.dto.ekedcdto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CriteriaTypeResponse {

    private String code;
    private String msgUser;
    private String msgDeveloper;
    private List<CriteriaType> criteriaTypes;
}
