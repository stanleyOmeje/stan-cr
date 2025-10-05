package com.stan.stancore.extended.dto.request;

import com.systemspecs.remita.vending.vendingcommon.enums.FieldType;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UpdateFieldDTO {

    private String description;

    @NotBlank
    private FieldType type;

    private boolean required;


}
