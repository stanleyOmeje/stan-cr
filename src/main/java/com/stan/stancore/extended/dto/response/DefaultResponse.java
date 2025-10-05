package com.stan.stancore.extended.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Data
public class DefaultResponse {

    private String message;
    private String status;

    private Object data;

    public DefaultResponse() {}

    public DefaultResponse(String message, String status) {
        this.message = message;
        this.status = status;
    }

    public DefaultResponse(String message, String status, Object data) {
        this.message = message;
        this.status = status;
        this.data = data;
    }
}
