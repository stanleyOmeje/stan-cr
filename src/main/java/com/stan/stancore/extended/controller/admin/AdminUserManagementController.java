package com.stan.stancore.extended.controller.admin;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.vending.extended.service.UserService;
import com.systemspecs.remita.vending.vendingcommon.dto.request.AdminUserRequest;
import com.systemspecs.remita.vending.vendingcommon.dto.response.AdminResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/admin/user")
@RestController
@RequiredArgsConstructor
public class AdminUserManagementController {

    private final UserService userService;

    @ActivityTrail
    @PostMapping("/create")
    public ResponseEntity<AdminResponse> addUser(@RequestBody @Valid AdminUserRequest request) {
        return userService.addUser(request);
    }

    @ActivityTrail
    @PatchMapping("/modify")
    public ResponseEntity<AdminResponse> updateUser(@RequestBody @Valid AdminUserRequest request) {
        return userService.updateUser(request);
    }

    @ActivityTrail
    @PostMapping("/search")
    public ResponseEntity<AdminResponse> getUser(@RequestBody @Valid AdminUserRequest request) {
        return userService.getUser(request);
    }
}
