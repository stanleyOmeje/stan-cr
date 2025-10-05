package com.stan.stancore.extended.dto.request;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CreateProviderRequest {

    private String code;
    private String name;
    private String description;
    private String logoUrl;
    private List<Long> categoryList = new ArrayList<>();
}
