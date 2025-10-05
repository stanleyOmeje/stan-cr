package com.stan.stancore.extended.exception;

import com.systemspecs.remita.vending.extended.enums.ResponseStatus;

public class NotFoundException extends RuntimeException {

    private String code;

    public NotFoundException(String message, String code) {
        super(message);
        this.code = code;
    }

    public NotFoundException(String message) {
        super(message);
        this.code = ResponseStatus.NOT_FOUND.getCode();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
