package com.stan.stancore.extended.exception;

import com.systemspecs.remita.vending.extended.enums.ResponseStatus;

public class AlreadyExistException extends RuntimeException {

    private String code;

    public AlreadyExistException(String message) {
        super(message);
        this.code = ResponseStatus.ALREADY_EXIST.getCode();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
