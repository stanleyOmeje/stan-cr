package com.stan.stancore.extended.controller.admin;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.vending.extended.dto.ekedcdto.request.*;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.service.EkedcService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/admin/admin")
@RestController
@RequiredArgsConstructor
public class EkedcController {

    private final EkedcService ekedcService;

    @ActivityTrail
    @PostMapping("/create/user")
    public ResponseEntity<DefaultResponse> createUser(@RequestBody @Valid CreateUserRequest createUserRequest) {
        return ekedcService.createUser(createUserRequest);
    }

    @ActivityTrail
    @PatchMapping("/modify/user")
    public ResponseEntity<DefaultResponse> modifyUser(@RequestBody @Valid ModifyUserRequest request) {
        return ekedcService.modifyUser(request);
    }

    @ActivityTrail
    @PostMapping("/user")
    public ResponseEntity<DefaultResponse> searchUser(@RequestBody @Valid SearchUserRequest request) {
        return ekedcService.searchUser(request);
    }

    @ActivityTrail
    @PostMapping("/validate-password")
    public ResponseEntity<DefaultResponse> validatePassword(@RequestBody @Valid ValidatePasswordRequest request) {
        return ekedcService.validatePassword(request);
    }

    @ActivityTrail
    @PatchMapping("/change-password")
    public ResponseEntity<DefaultResponse> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        return ekedcService.changePassword(request);
    }

    @ActivityTrail
    @PatchMapping("/forgot-password")
    public ResponseEntity<DefaultResponse> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        return ekedcService.forgotPassword(request);
    }

    @ActivityTrail
    @PostMapping("/criteria")
    public ResponseEntity<DefaultResponse> fetchCriteriaType(@RequestBody @Valid CriteriaTypeRequest request) {
        return ekedcService.fetchCriteriaType(request);
    }

    @ActivityTrail
    @PostMapping("/customer/enquiry")
    public ResponseEntity<DefaultResponse> customerEnquiry(@RequestBody @Valid CustomerEnquiryRequest request) {
        return ekedcService.customerEnquiry(request);
    }

    @ActivityTrail
    @PostMapping("/shift/enquiry")
    public ResponseEntity<DefaultResponse> shiftEnquiry(@RequestBody @Valid ShiftEnquiryRequest request) {
        return ekedcService.shiftEnquiry(request);
    }

    @ActivityTrail
    @PostMapping("/vendorinfo")
    public ResponseEntity<DefaultResponse> vendorInfo(@RequestBody @Valid VendorInformationRequest request) {
        return ekedcService.vendorInfo(request);
    }

    @ActivityTrail
    @PostMapping("/vendor/transaction")
    public ResponseEntity<DefaultResponse> vendorTransaction(@RequestBody @Valid VendorTransactionRequest request) {
        return ekedcService.vendorTransaction(request);
    }

    @ActivityTrail
    @PostMapping("/calculate/payment")
    public ResponseEntity<DefaultResponse> calculatePayment(@RequestBody @Valid CalculatePaymentRequest request) {
        return ekedcService.calculatePayment(request);
    }

    @ActivityTrail
    @GetMapping("/wallet/history")
    public ResponseEntity<DefaultResponse> getWalletHistory(@RequestBody @Valid WalletHistoryRequest request) {
        return ekedcService.walletHistory(request);
    }

    @ActivityTrail
    @PostMapping("/vendor/topups")
    public ResponseEntity<DefaultResponse> vendorTopUps(@RequestBody @Valid VendorTopUpsRequest request) {
        return ekedcService.vendorTopUps(request);
    }

    @ActivityTrail
    @PostMapping("/vendor/statement")
    public ResponseEntity<DefaultResponse> vendorStatement(@RequestBody @Valid VendorStatementRequest request) {
        return ekedcService.vendorStatement(request);
    }
}
