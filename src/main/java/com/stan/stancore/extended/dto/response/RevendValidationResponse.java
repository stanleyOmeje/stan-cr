package com.stan.stancore.extended.dto.response;

import com.systemspecs.remita.vending.vendingcommon.entity.Transaction;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RevendValidationResponse {

    List<Transaction> transactionList;
    List<RevendErrorMap> errorResponse = new ArrayList<>();
}
