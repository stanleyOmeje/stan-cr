package com.stan.stancore.extended.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemspecs.remita.vending.extended.dto.JWTPojo;
import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletRequest;

public class JwtUtil {

    public static JWTPojo decodeJwt(String jwtToken) {
        String[] split_string = jwtToken.split("\\.");
        String base64EncodedHeader = split_string[0];
        String base64EncodedBody = split_string[1];
        //String base64EncodedSignature = split_string[2];
        Base64 base64Url = new Base64(true);
        String header = new String(base64Url.decode(base64EncodedHeader));
        String body = new String(base64Url.decode(base64EncodedBody));
        ObjectMapper objectMapper = new ObjectMapper();
        JWTPojo jwtPojo = null;
        try {
            jwtPojo = objectMapper.readValue(body, JWTPojo.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        System.out.println(jwtPojo);
        return jwtPojo;
    }

    public static Long extractProfileId(HttpServletRequest request) {
        String token = request.getHeader(HttpHeaders.AUTHORIZATION).substring(7);
        JWTPojo decodedJwt = decodeJwt(token);
        return decodedJwt.getUser().getId();
    }

    public static String getToken(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.AUTHORIZATION).substring(7);
    }

    public static String getProfileId(HttpServletRequest request) {
        String token = request.getHeader(HttpHeaders.AUTHORIZATION).substring(7);
        JWTPojo decodedJwt = decodeJwt(token);
        return String.valueOf(decodedJwt.getUser().getId());
    }

    public static String getProfileType(HttpServletRequest request) {
        String token = request.getHeader(HttpHeaders.AUTHORIZATION).substring(7);
        JWTPojo decodedJwt = decodeJwt(token);
        return decodedJwt.getUser().getProfileType();
    }

    public static String getUserEmail(HttpServletRequest request) {
        String token = request.getHeader(HttpHeaders.AUTHORIZATION).substring(7);
        JWTPojo decodedJwt = decodeJwt(token);
        return decodedJwt.getUser().getEmail();
    }

    public static String[] getAuthRoles(HttpServletRequest request) {
        String token = request.getHeader(HttpHeaders.AUTHORIZATION).substring(7);
        JWTPojo decodedJwt = decodeJwt(token);
        return decodedJwt.getAuth().split(",");
    }
}
