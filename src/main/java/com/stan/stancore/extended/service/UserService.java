package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.vendingcommon.dto.request.AdminUserRequest;
import com.systemspecs.remita.vending.vendingcommon.dto.response.AdminResponse;
import org.springframework.http.ResponseEntity;

public interface UserService {
    ResponseEntity<AdminResponse> addUser(AdminUserRequest request);

    ResponseEntity<AdminResponse> updateUser(AdminUserRequest request);

    ResponseEntity<AdminResponse> getUser(AdminUserRequest request);
}
