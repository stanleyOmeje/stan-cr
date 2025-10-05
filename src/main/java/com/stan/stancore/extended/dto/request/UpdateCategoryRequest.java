package com.stan.stancore.extended.dto.request;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class UpdateCategoryRequest {

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    public UpdateCategoryRequest() {}

    public UpdateCategoryRequest(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
