package com.stan.stancore.extended.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class FundRecoupErrorResponse {
    List<FundRecoupErrorMap> errorResponse = new ArrayList<>();
}
