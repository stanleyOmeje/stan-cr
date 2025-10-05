package com.stan.stancore.extended.util;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class MailMessageRequest implements Serializable {
    private static final long serialVersionUID = 3235873067528545775L;
    private String requestId;
    private Map<String, Object> messageMap;
}
