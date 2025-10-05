package com.stan.stancore.extended.controller.admin;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.vending.extended.service.AuthService;
import com.systemspecs.remita.vending.vendingcommon.dto.request.AdminAuthRequest;
import com.systemspecs.remita.vending.vendingcommon.dto.response.AdminResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/admin/auth")
@RestController
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;

    @ActivityTrail
    @PostMapping("/login")
    public ResponseEntity<AdminResponse> getToken(@RequestBody @Valid AdminAuthRequest request) {
        return authService.performLogin(request);
    }

    @ActivityTrail
    @PostMapping("/validate-password")
    public ResponseEntity<AdminResponse> validatePassword(@RequestBody @Valid AdminAuthRequest request) {
        return authService.verifyPassword(request);
    }

    @ActivityTrail
    @PatchMapping("/change-password")
    public ResponseEntity<AdminResponse> changePassword(@RequestBody @Valid AdminAuthRequest request) {
        return authService.modifyPassword(request);
    }

    @ActivityTrail
    @PatchMapping("/forgot-password")
    public ResponseEntity<AdminResponse> forgotPassword(@RequestBody @Valid AdminAuthRequest request) {
        return authService.forgotPassword(request);
    }
}
