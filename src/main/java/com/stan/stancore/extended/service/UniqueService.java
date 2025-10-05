package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.vendingcommon.dto.request.AdminUniqueRequest;
import com.systemspecs.remita.vending.vendingcommon.dto.response.AdminResponse;
import org.springframework.http.ResponseEntity;

public interface UniqueService {
    ResponseEntity<AdminResponse> performCustomerEnquiry(AdminUniqueRequest adminUniqueRequest);
    ResponseEntity<AdminResponse> performShiftEnquiry(AdminUniqueRequest adminUniqueRequest);
    ResponseEntity<AdminResponse> perfomVendorInformation(AdminUniqueRequest adminUniqueRequest);
    ResponseEntity<AdminResponse> performCriteriaType(AdminUniqueRequest adminUniqueRequest);
}
