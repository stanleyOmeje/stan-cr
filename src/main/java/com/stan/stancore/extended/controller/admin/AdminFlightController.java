package com.stan.stancore.extended.controller.admin;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.service.FlightService;
import com.systemspecs.remita.vending.vendingcommon.flight.dto.request.CabinClassRequest;
import com.systemspecs.remita.vending.vendingcommon.flight.dto.request.CancelFlightRequest;
import com.systemspecs.remita.vending.vendingcommon.flight.dto.request.ConvertCurrencyRequest;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/admin/flight")
@RestController
@RequiredArgsConstructor
public class AdminFlightController {

    private final FlightService flightService;

    @ActivityTrail
    @GetMapping("/airports/{productCode}")
    public ResponseEntity<DefaultResponse> getAirport(@PathVariable String productCode) {
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse = flightService.getAirport();
        return ResponseEntity.ok(defaultResponse);
    }

    @ActivityTrail
    @PostMapping("/airline/{processorId}")
    public ResponseEntity<DefaultResponse> getAirlines(String processorId) {
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse = flightService.getAirlines(processorId);
        return ResponseEntity.ok(defaultResponse);
    }

    @ActivityTrail
    @PostMapping("/currency")
    public ResponseEntity<DefaultResponse> convertCurrency(@RequestBody @Valid ConvertCurrencyRequest convertCurrencyRequest) {
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse = flightService.convertCurrency(convertCurrencyRequest);
        return ResponseEntity.ok(defaultResponse);
    }

    @ActivityTrail
    @PostMapping("/cancel")
    public ResponseEntity<DefaultResponse> cancelFlight(@RequestBody @Valid CancelFlightRequest cancelFlightRequest) {
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse = flightService.cancelFlight(cancelFlightRequest);
        return ResponseEntity.ok(defaultResponse);
    }

    @ActivityTrail
    @GetMapping("/balance/{processorId}")
    public ResponseEntity<DefaultResponse> getBalance(@PathVariable String processorId) {
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse = flightService.getWalletBalance(processorId);
        return ResponseEntity.ok(defaultResponse);
    }

    @ActivityTrail
    @PostMapping("/cabin/{processorId}")
    public ResponseEntity<DefaultResponse> createOrUpdateCabinClass(
        @RequestBody @Valid CabinClassRequest request,
        @PathVariable String processorId
    ) {
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse = flightService.createCabin(request, processorId);
        return ResponseEntity.ok(defaultResponse);
    }

    @ActivityTrail
    @GetMapping("/cabin")
    public ResponseEntity<DefaultResponse> getCabin() {
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse = flightService.fetchCabin();
        return ResponseEntity.ok(defaultResponse);
    }
}
