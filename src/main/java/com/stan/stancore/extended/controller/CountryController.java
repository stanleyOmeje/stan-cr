package com.stan.stancore.extended.controller;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.service.CountryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vending")
@RequiredArgsConstructor
public class CountryController {

    private final CountryService countryService;

    @ActivityTrail
    @GetMapping("/countries")
    public ResponseEntity<DefaultResponse> fetchCountries(
        @RequestParam(required = false, defaultValue = "1") int page,
        @RequestParam(required = false, defaultValue = "100") int perPage
    ) {
        return countryService.fetchCountries(page, perPage);
    }

    @ActivityTrail
    @GetMapping("/countries/{isoCode}")
    public ResponseEntity<DefaultResponse> fetchCountryByIsoCode(@PathVariable("isoCode") String isoCode) {
        return countryService.fetchCountryByIsoCode(isoCode);
    }
}
