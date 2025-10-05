package com.stan.stancore.extended.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.systemspecs.remita.vending.vendingcommon.entity.Category;
import lombok.Data;

import javax.persistence.ManyToOne;
import java.util.Set;

@Data
public class DisplayProvider {

    private String code;
    private String name;
    private String description;
    private String logoUrl;

    @JsonIgnore
    @ManyToOne
    private Set<Category> category;
}
