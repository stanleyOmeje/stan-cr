package com.stan.stancore.extended.controller;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.dto.auth.MerchantDetailsDto;
import com.systemspecs.remita.enumeration.ServicesEnum;
import com.systemspecs.remita.sdk.auth.CoreSDKAuth;
import com.systemspecs.remita.security.SecurityUtils;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.exception.BadRequestException;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.MoviesService;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import com.systemspecs.remita.vending.vendingcommon.movies.dto.request.*;
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
@RequestMapping("/api/v1/vending/movies")
@RestController
public class MoviesController {

    private final MoviesService moviesService;
    private final SecurityUtils securityUtils;
    private final CoreSDKAuth coreSDKAuth;

    @ActivityTrail
    @GetMapping
    public ResponseEntity<DefaultResponse> getAllMovie(HttpServletRequest request, @ParameterObject MovieParam movieParam) {
        log.info(">>> Inside MovieController::getAllMovie with Request ...{}", movieParam);
        DefaultResponse defaultResponse = new DefaultResponse();
        Optional<MerchantDetailsDto> merchantDetailsDto = Optional.of(new MerchantDetailsDto());
        try {
            log.info("Inside try catch");
            securityUtils.containsSecretKey(request);
            merchantDetailsDto = getAuthentication(request);
            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("Merchant details is not found");
            }
            defaultResponse = moviesService.getMovies(movieParam);
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
    @GetMapping("/{slug}")
    public ResponseEntity<DefaultResponse> getSingleMovie(HttpServletRequest request, @PathVariable("slug") String slug) {
        log.info(">>> Inside MovieController::getSingleMovie with Request ...{}", slug);
        DefaultResponse defaultResponse = new DefaultResponse();
        Optional<MerchantDetailsDto> merchantDetailsDto = Optional.of(new MerchantDetailsDto());
        try {
            securityUtils.containsSecretKey(request);
            merchantDetailsDto = getAuthentication(request);
            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("Merchant details is not found");
            }
            GetMovieByName getMovieByName = new GetMovieByName();
            getMovieByName.setName(slug);
            defaultResponse = moviesService.getSingleMovie(getMovieByName);
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
    @GetMapping("/showtime")
    public ResponseEntity<DefaultResponse> getMovieShowTime(
        HttpServletRequest request,
        @RequestParam("movieSlug") String movieSlug,
        @RequestParam("cinemaSlug") String cinemaSlug
    ) {
        log.info(">>> Inside MovieController::getMovieShowTime with Request ...{}...{}", movieSlug, cinemaSlug);
        DefaultResponse defaultResponse = new DefaultResponse();
        Optional<MerchantDetailsDto> merchantDetailsDto = Optional.of(new MerchantDetailsDto());
        try {
            securityUtils.containsSecretKey(request);
            merchantDetailsDto = getAuthentication(request);
            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("Merchant details is not found");
            }
            GetShowTimeRequest showTimeRequest = new GetShowTimeRequest();
            showTimeRequest.setMovieName(movieSlug);
            showTimeRequest.setCinemaName(cinemaSlug);

            defaultResponse = moviesService.getMovieShowTime(showTimeRequest);
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
    public ResponseEntity<DefaultResponse> createMovieBooking(
        HttpServletRequest request,
        @RequestBody @Valid CreateMovieBookingRequest createMovieBookingRequest
    ) {
        log.info(">>> Inside MovieController::createMovieBooking with Request ...{}", createMovieBookingRequest);
        DefaultResponse defaultResponse = new DefaultResponse();
        Optional<MerchantDetailsDto> merchantDetailsDto = Optional.of(new MerchantDetailsDto());
        try {
            securityUtils.containsSecretKey(request);
            merchantDetailsDto = getAuthentication(request);
            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("Merchant details is not found");
            }

            defaultResponse = moviesService.createMovieBooking(createMovieBookingRequest);
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

    @ActivityTrail
    @PostMapping("/book")
    public ResponseEntity<DefaultResponse> completeMovieBooking(
        HttpServletRequest request,
        @RequestBody @Valid CompleteMovieBookingRequest completeMovieBookingRequest
    ) {
        log.info(">>> Inside MovieController::completeMovieBooking with Request ...{}", completeMovieBookingRequest);
        DefaultResponse defaultResponse = new DefaultResponse();
        Optional<MerchantDetailsDto> merchantDetailsDto = Optional.of(new MerchantDetailsDto());
        try {
            securityUtils.containsSecretKey(request);
            merchantDetailsDto = getAuthentication(request);
            if (merchantDetailsDto.isEmpty()) {
                throw new NotFoundException("Merchant details is not found");
            }

            defaultResponse = moviesService.completeMovieBooking(completeMovieBookingRequest, merchantDetailsDto.get());
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
}
