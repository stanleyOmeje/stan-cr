package com.stan.stancore.extended.controller;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.dto.auth.MerchantDetailsDto;
import com.systemspecs.remita.enumeration.ServicesEnum;
import com.systemspecs.remita.sdk.auth.CoreSDKAuth;
import com.systemspecs.remita.security.SecurityUtils;
import com.systemspecs.remita.vending.extended.config.VendingCoreProperties;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.exception.BadRequestException;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.FlightService;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import com.systemspecs.remita.vending.vendingcommon.flight.dto.request.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/vending/flight")
@RestController
public class FlightController {

    private final FlightService flightService;
    private final SecurityUtils securityUtils;
    private final CoreSDKAuth coreSDKAuth;
    private final VendingCoreProperties properties;

    @ActivityTrail
    @PostMapping("/search")
    public ResponseEntity<DefaultResponse> searchFlight(
        HttpServletRequest request,
        @RequestBody @Valid SearchFlightRequest searchFlightRequest
    ) {
        log.info(">>> Inside FlightController::searchFlight with Request ...{}", searchFlightRequest);
        DefaultResponse defaultResponse = new DefaultResponse();
        Optional<MerchantDetailsDto> merchantDetailsDto = Optional.of(new MerchantDetailsDto());
        try {
            securityUtils.containsSecretKey(request);
            merchantDetailsDto = getAuthentication(request);
            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("Merchant details is not found");
            }
            defaultResponse = flightService.searchFlight(searchFlightRequest);
            return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (NotFoundException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            defaultResponse.setStatus(TransactionStatus.AUTHENTICATION_ERROR.getCode());
            defaultResponse.setMessage(TransactionStatus.AUTHENTICATION_ERROR.getMessage());
            e.printStackTrace();
        }
        return ResponseEntity.ok(defaultResponse);
    }

    @ActivityTrail
    @PostMapping("/validate")
    public ResponseEntity<DefaultResponse> validateFlight(HttpServletRequest request, @RequestBody @Valid ValidateFlight validateFlight) {
        log.info(">>> Inside FlightController::confirmPrice with Request ...{}", validateFlight);
        DefaultResponse defaultResponse = new DefaultResponse();
        Optional<MerchantDetailsDto> merchantDetailsDto = Optional.of(new MerchantDetailsDto());
        try {
            securityUtils.containsSecretKey(request);
            merchantDetailsDto = getAuthentication(request);
            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("Merchant details is not found");
            }

            defaultResponse = flightService.validateFlight(validateFlight);
            return ResponseEntity.ok(defaultResponse);
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (NotFoundException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            defaultResponse.setStatus(TransactionStatus.AUTHENTICATION_ERROR.getCode());
            defaultResponse.setMessage(TransactionStatus.AUTHENTICATION_ERROR.getMessage());
            e.printStackTrace();
        }
        return ResponseEntity.ok(defaultResponse);
    }

    @ActivityTrail
    @PostMapping("/book")
    public ResponseEntity<DefaultResponse> bookFlight(HttpServletRequest request, @RequestBody @Valid BookFlightRequest bookFlightRequest) {
        log.info(">>> Inside FlightController::bookFlight with Request ...{}", bookFlightRequest);
        DefaultResponse defaultResponse = new DefaultResponse();
        Optional<MerchantDetailsDto> merchantDetailsDto = Optional.of(new MerchantDetailsDto());
        try {
            securityUtils.containsSecretKey(request);
            merchantDetailsDto = getAuthentication(request);
            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("Merchant details is not found");
            }

            defaultResponse = flightService.bookFlight(bookFlightRequest, merchantDetailsDto.get());
            return ResponseEntity.ok(defaultResponse);
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (NotFoundException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            defaultResponse.setStatus(TransactionStatus.AUTHENTICATION_ERROR.getCode());
            defaultResponse.setMessage(TransactionStatus.AUTHENTICATION_ERROR.getMessage());
            e.printStackTrace();
        }
        return ResponseEntity.ok(defaultResponse);
    }

    @ActivityTrail
    @PostMapping("/booking-details")
    public ResponseEntity<DefaultResponse> getBookedFlightDetails(
        HttpServletRequest request,
        @RequestBody @Valid BookingDetailsRequest bookingDetailsRequest
    ) {
        log.info(">>> Inside FlightController::getBookedFlightDetails with Request ...{}", bookingDetailsRequest);
        DefaultResponse defaultResponse = new DefaultResponse();
        Optional<MerchantDetailsDto> merchantDetailsDto = Optional.of(new MerchantDetailsDto());
        try {
            securityUtils.containsSecretKey(request);
            merchantDetailsDto = getAuthentication(request);
            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("Merchant details is not found");
            }

            defaultResponse = flightService.getBookedFlightDetails(bookingDetailsRequest);
            return ResponseEntity.ok(defaultResponse);
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (NotFoundException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            defaultResponse.setStatus(TransactionStatus.AUTHENTICATION_ERROR.getCode());
            defaultResponse.setMessage(TransactionStatus.AUTHENTICATION_ERROR.getMessage());
            e.printStackTrace();
        }
        return ResponseEntity.ok(defaultResponse);
    }

    @ActivityTrail
    @GetMapping("/airports")
    public ResponseEntity<DefaultResponse> getAirport(HttpServletRequest request) {
        log.info(">>> Inside FlightController::getAirport with Request ...");
        DefaultResponse defaultResponse = new DefaultResponse();
        Optional<MerchantDetailsDto> merchantDetailsDto = Optional.of(new MerchantDetailsDto());
        try {
            securityUtils.containsSecretKey(request);
            merchantDetailsDto = getAuthentication(request);
            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("Merchant details is not found");
            }
            defaultResponse = flightService.getAirport();
            return ResponseEntity.ok(defaultResponse);
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (NotFoundException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            defaultResponse.setStatus(TransactionStatus.AUTHENTICATION_ERROR.getCode());
            defaultResponse.setMessage(TransactionStatus.AUTHENTICATION_ERROR.getMessage());
            e.printStackTrace();
        }
        return ResponseEntity.ok(defaultResponse);
    }

    @ActivityTrail
    @GetMapping("/cabin")
    public ResponseEntity<DefaultResponse> getCabin(HttpServletRequest request) {
        log.info(">>> Inside FlightController::getCabin");
        DefaultResponse defaultResponse = new DefaultResponse();
        Optional<MerchantDetailsDto> merchantDetailsDto = Optional.of(new MerchantDetailsDto());
        try {
            securityUtils.containsSecretKey(request);
            merchantDetailsDto = getAuthentication(request);
            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("Merchant details is not found");
            }
            defaultResponse = flightService.fetchCabin();
            return ResponseEntity.ok(defaultResponse);
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (NotFoundException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            defaultResponse.setStatus(TransactionStatus.AUTHENTICATION_ERROR.getCode());
            defaultResponse.setMessage(TransactionStatus.AUTHENTICATION_ERROR.getMessage());
            e.printStackTrace();
        }
        return ResponseEntity.ok(defaultResponse);
    }

    private Optional<MerchantDetailsDto> getAuthentication(HttpServletRequest httpServletRequest) {
        Optional<MerchantDetailsDto> authResponse = coreSDKAuth.authenticateMerchant(httpServletRequest, ServicesEnum.VENDING);
        if (authResponse.isEmpty()) {
            throw new BadRequestException(
                TransactionStatus.AUTHENTICATION_ERROR.getMessage(),
                TransactionStatus.AUTHENTICATION_ERROR.getCode()
            );
        }
        return authResponse;
    }
}
