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
import com.systemspecs.remita.vending.extended.service.EventsTicketingService;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import com.systemspecs.remita.vending.vendingcommon.eventsticketing.dto.request.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.api.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/vending/events")
@RestController
public class EventsTicketingController {

    private final EventsTicketingService eventsTicketingService;
    private final SecurityUtils securityUtils;
    private final CoreSDKAuth coreSDKAuth;
    private final VendingCoreProperties properties;

    @ActivityTrail
    @GetMapping("/listings")
    public ResponseEntity<DefaultResponse> searchListing(HttpServletRequest request, @ParameterObject ListingParam listingParam) {
        log.info(">>> Inside EventsTicketingController::searchListing with Request ...{}", listingParam);
        DefaultResponse defaultResponse = new DefaultResponse();
        Optional<MerchantDetailsDto> merchantDetailsDto = Optional.of(new MerchantDetailsDto());
        try {
            log.info("Inside try catch");
            securityUtils.containsSecretKey(request);
            merchantDetailsDto = getAuthentication(request);
            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("Merchant details is not found");
            }
            defaultResponse = eventsTicketingService.getListing(listingParam);
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
    public ResponseEntity<DefaultResponse> validateListing(
        HttpServletRequest request,
        @RequestBody @Valid SingleListingRequest singleListingRequest
    ) {
        log.info(">>> Inside EventsTicketingController::getSingleListing with Request ...{}", singleListingRequest);
        DefaultResponse defaultResponse = new DefaultResponse();
        Optional<MerchantDetailsDto> merchantDetailsDto = Optional.of(new MerchantDetailsDto());
        try {
            securityUtils.containsSecretKey(request);
            merchantDetailsDto = getAuthentication(request);
            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("Merchant details is not found");
            }

            defaultResponse = eventsTicketingService.getSingleListing(singleListingRequest);
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
    @PostMapping("/location-ticket-booking")
    public ResponseEntity<DefaultResponse> createLocationTicket(
        HttpServletRequest request,
        @RequestBody @Valid LocationTicketBookingRequest locationTicketBookingRequest
    ) {
        log.info(">>> Inside EventsTicketingController::bookLocationTicket with Request ...{}", locationTicketBookingRequest);
        DefaultResponse defaultResponse = new DefaultResponse();
        Optional<MerchantDetailsDto> merchantDetailsDto = Optional.of(new MerchantDetailsDto());
        try {
            securityUtils.containsSecretKey(request);
            merchantDetailsDto = getAuthentication(request);
            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("Merchant details is not found");
            }

            defaultResponse = eventsTicketingService.createLocationBooking(locationTicketBookingRequest, merchantDetailsDto.get());
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
    @PostMapping("/create-booking")
    public ResponseEntity<DefaultResponse> createEventBooking(
        HttpServletRequest request,
        @RequestBody @Valid EventTicketBookingRequest eventTicketBookingRequest
    ) {
        log.info(">>> Inside EventsTicketingController::BookedEventTicket with Request ...{}", eventTicketBookingRequest);
        DefaultResponse defaultResponse = new DefaultResponse();
        Optional<MerchantDetailsDto> merchantDetailsDto = Optional.of(new MerchantDetailsDto());
        try {
            securityUtils.containsSecretKey(request);
            merchantDetailsDto = getAuthentication(request);
            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("Merchant details is not found");
            }

            defaultResponse = eventsTicketingService.createEventBooking(eventTicketBookingRequest);
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
    public ResponseEntity<DefaultResponse> completeEventBooking(
        HttpServletRequest request,
        @RequestBody @Valid CompleteEventBookingRequest completeEventBookingRequest
    ) {
        log.info(">>> Inside EventsTicketingController::completeEventBooking with Request ...{}", completeEventBookingRequest);
        DefaultResponse defaultResponse = new DefaultResponse();
        Optional<MerchantDetailsDto> merchantDetailsDto = Optional.of(new MerchantDetailsDto());
        try {
            securityUtils.containsSecretKey(request);
            merchantDetailsDto = getAuthentication(request);
            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("Merchant details is not found");
            }

            defaultResponse = eventsTicketingService.completeEventBooking(completeEventBookingRequest, merchantDetailsDto.get());
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
    @GetMapping("/categories")
    public ResponseEntity<DefaultResponse> getEventCategory(HttpServletRequest request) {
        log.info(">>> Inside EventsTicketingController::getEventCategory with Request ...{}");
        DefaultResponse defaultResponse = new DefaultResponse();
        Optional<MerchantDetailsDto> merchantDetailsDto = Optional.of(new MerchantDetailsDto());
        try {
            securityUtils.containsSecretKey(request);
            merchantDetailsDto = getAuthentication(request);
            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("Merchant details is not found");
            }
            defaultResponse = eventsTicketingService.getCategory();
            return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (NotFoundException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            log.info("Inside categoriesController::getEventCategory");
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
