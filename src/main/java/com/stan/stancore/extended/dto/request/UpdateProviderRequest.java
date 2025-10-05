package com.stan.stancore.extended.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class UpdateProviderRequest {

    private String name;
    private String description;
    private String logoUrl;
    private List<Long> categoryList;
}
