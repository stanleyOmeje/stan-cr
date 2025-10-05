package com.stan.stancore.extended.exception;

public class UnknownException extends RuntimeException {

    private String code;

    public UnknownException(String message, String code) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
