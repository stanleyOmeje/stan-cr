package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.vendingcommon.dto.request.AdminAuthRequest;
import com.systemspecs.remita.vending.vendingcommon.dto.response.AdminResponse;
import org.springframework.http.ResponseEntity;

public interface AuthService {
    ResponseEntity<AdminResponse> performLogin(AdminAuthRequest adminAuthRequest);
    ResponseEntity<AdminResponse> verifyPassword(AdminAuthRequest adminAuthRequest);
    ResponseEntity<AdminResponse> modifyPassword(AdminAuthRequest adminAuthRequest);
    ResponseEntity<AdminResponse> forgotPassword(AdminAuthRequest adminAuthRequest);
}
