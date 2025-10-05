package com.stan.stancore.extended.service;

import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import org.springframework.http.ResponseEntity;

public interface CountryService {
    ResponseEntity<DefaultResponse>fetchCountries(int page, int perPage);
    ResponseEntity<DefaultResponse> fetchCountryByIsoCode(String isoCode);
}
