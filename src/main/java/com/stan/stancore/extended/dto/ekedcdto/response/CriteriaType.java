package com.stan.stancore.extended.dto.ekedcdto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CriteriaType {

    public String codType;
    public String type;

    @JsonProperty("default")
    public int mydefault;
}
