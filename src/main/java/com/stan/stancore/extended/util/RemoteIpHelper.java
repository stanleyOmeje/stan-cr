package com.stan.stancore.extended.util;

import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RemoteIpHelper {

    private static final String UNKNOWN = "unknown";

    private static final String[] IP_HEADER_CANDIDATES = {
        "CF-Connecting-IP",
        "True-Client-IP",
        "X-Forwarded-For",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "HTTP_VIA",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "REMOTE_ADDR",
    };

    public static String getRemoteIpFrom(HttpServletRequest request) {
        for (String header : IP_HEADER_CANDIDATES) {
            String ip = request.getHeader(header);
            if (isIpFound(ip)) {
                return ip;
            }
        }
        return request.getRemoteHost();
    }

    private static boolean isIpFound(String ip) {
        return ip != null && ip.length() > 0 && !UNKNOWN.equalsIgnoreCase(ip);
    }

    public static List<String> getRemoteIdentifierFrom(HttpServletRequest request, String requestIdentifierName) {
        List<String> identifier = new ArrayList<>();

        List<String> requestIdentifiers;
        if (StringUtils.isNotBlank(requestIdentifierName)) {
            requestIdentifiers = Arrays.asList(requestIdentifierName.split(","));
            for (String getIdentified : requestIdentifiers) {
                identifier.add(getIdentified);
                identifier.add(request.getHeader(getIdentified));
            }
        } else {
            identifier.add("CLIENT_IP");
            identifier.add(getRemoteIpFrom(request));
        }
        return identifier;
    }
}
