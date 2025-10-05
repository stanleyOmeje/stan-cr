package com.stan.stancore.extended.dto.response;

import lombok.Data;

@Data
public class VendingItemListResponse {

    private long totalPage;
    private long totalContent;
    private long successCount;
    private long failedCount;
    private long pendingCount;
    private Object items;
}
