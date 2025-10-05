package com.stan.stancore.extended.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class CreateCategoryRequest extends UpdateCategoryRequest {

    @NotBlank
    private String code;

    public CreateCategoryRequest() {}

    public CreateCategoryRequest(String name, String description, String code) {
        super(name, description);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
