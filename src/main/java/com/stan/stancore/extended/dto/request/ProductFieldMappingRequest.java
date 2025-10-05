package com.stan.stancore.extended.dto.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class ProductFieldMappingRequest {

    @NotBlank
    private String productCode;

    @NotBlank
    private String uniqueFieldName;

    private String emailFieldName;
    private String phoneNumberFieldName;
}
