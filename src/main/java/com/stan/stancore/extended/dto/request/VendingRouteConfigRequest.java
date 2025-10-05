package com.stan.stancore.extended.dto.request;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class VendingRouteConfigRequest {

    private String productCode;

    private String processorId;

    private Boolean active;

    private Boolean enableFallbackProcessor;

    private String fallbackProcessorId;

    private String systemProductType;
}
