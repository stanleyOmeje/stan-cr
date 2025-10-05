package com.stan.stancore.extended.enums;

public enum Templates {
    PRODUCT("product.csv"),
    REVEND("revend.csv");

    public final String path;

    private Templates(String path) {
        this.path = path;
    }
}
