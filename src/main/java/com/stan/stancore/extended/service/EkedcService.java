package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.extended.dto.ekedcdto.request.*;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import org.springframework.http.ResponseEntity;

import javax.validation.Valid;

public interface EkedcService {
    ResponseEntity<DefaultResponse> createUser(CreateUserRequest request);
    ResponseEntity<DefaultResponse> modifyUser(ModifyUserRequest request);
    ResponseEntity<DefaultResponse> searchUser(SearchUserRequest request);
    ResponseEntity<DefaultResponse> validatePassword(ValidatePasswordRequest request);

    ResponseEntity<DefaultResponse> changePassword(ChangePasswordRequest request);

    ResponseEntity<DefaultResponse> forgotPassword(ForgotPasswordRequest request);

    ResponseEntity<DefaultResponse> fetchCriteriaType(CriteriaTypeRequest request);

    ResponseEntity<DefaultResponse> customerEnquiry(CustomerEnquiryRequest request);

    ResponseEntity<DefaultResponse> shiftEnquiry(ShiftEnquiryRequest request);

    ResponseEntity<DefaultResponse> vendorInfo(VendorInformationRequest request);

    ResponseEntity<DefaultResponse> vendorTransaction(VendorTransactionRequest request);

    ResponseEntity<DefaultResponse> calculatePayment(CalculatePaymentRequest request);

    ResponseEntity<DefaultResponse> walletHistory(WalletHistoryRequest request);

    ResponseEntity<DefaultResponse> vendorTopUps(@Valid VendorTopUpsRequest request);

    ResponseEntity<DefaultResponse> vendorStatement(@Valid VendorStatementRequest request);
}
