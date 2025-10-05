package com.stan.stancore.extended.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class BulkDebitListRequest {
    private List<FundRecoupRequest> items;
}
