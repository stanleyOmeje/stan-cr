package com.stan.stancore.extended.dto.response;

import lombok.Data;

@Data
public class ListTransactionResponse {

    private long totalPage;
    private long totalContent;
    private Object items;
}
