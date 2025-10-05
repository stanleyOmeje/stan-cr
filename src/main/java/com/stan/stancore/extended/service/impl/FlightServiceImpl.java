package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.dto.account.AccountDebitRequest;
import com.systemspecs.remita.dto.auth.MerchantDetailsDto;
import com.systemspecs.remita.enumeration.CoreSDKResult;
import com.systemspecs.remita.enumeration.VendProcessor;
import com.systemspecs.remita.vending.extended.dto.BookResponse;
import com.systemspecs.remita.vending.extended.dto.CommissionDTO;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.BadRequestException;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.exception.UnknownException;
import com.systemspecs.remita.vending.extended.service.FlightService;
import com.systemspecs.remita.vending.extended.util.*;
import com.systemspecs.remita.vending.vendingcommon.dto.request.ProcessorPackageRequest;
import com.systemspecs.remita.vending.vendingcommon.dto.request.TransactionRequest;
import com.systemspecs.remita.vending.vendingcommon.entity.FundRecoup;
import com.systemspecs.remita.vending.vendingcommon.entity.Product;
import com.systemspecs.remita.vending.vendingcommon.entity.VendingServiceRouteConfig;
import com.systemspecs.remita.vending.vendingcommon.enums.SubscriptionType;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import com.systemspecs.remita.vending.vendingcommon.flight.dto.request.*;
import com.systemspecs.remita.vending.vendingcommon.flight.dto.response.*;
import com.systemspecs.remita.vending.vendingcommon.flight.entity.BookingValidation;
import com.systemspecs.remita.vending.vendingcommon.flight.entity.Processors;
import com.systemspecs.remita.vending.vendingcommon.flight.enums.BookingStatus;
import com.systemspecs.remita.vending.vendingcommon.flight.factory.FlightProductVendingServiceDelegateBean;
import com.systemspecs.remita.vending.vendingcommon.flight.repository.BookingValidationRepository;
import com.systemspecs.remita.vending.vendingcommon.flight.repository.ProcRepository;
import com.systemspecs.remita.vending.vendingcommon.flight.service.FlightProductAbstractVendingService;
import com.systemspecs.remita.vending.vendingcommon.repository.FundRecoupRepository;
import com.systemspecs.remita.vending.vendingcommon.repository.ProductRepository;
import com.systemspecs.remita.vending.vendingcommon.repository.TransactionRepository;
import com.systemspecs.remita.vending.vendingcommon.repository.VendingServiceRouteConfigRepository;
import com.systemspecs.remita.vending.vendingcommon.service.VendingServiceProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import static com.systemspecs.remita.vending.extended.enums.ResponseStatus.INTERNAL_SERVER_ERROR;
import static com.systemspecs.remita.vending.extended.enums.ResponseStatus.NOT_FOUND;

@Slf4j
@RequiredArgsConstructor
@Service
public class FlightServiceImpl implements FlightService {

    private final ProductRepository productRepository;
    private final FlightProductVendingServiceDelegateBean flightProductVendingServiceDelegateBean;
    private final VendingServiceProcessorService vendingServiceProcessorService;
    private final BookingValidationRepository bookingValidationRepository;
    private final AccountUtil accountUtil;
    private final CommissionUtil commissionUtil;
    private final ProcRepository procRepository;
    private final TransactionRepository transactionRepository;
    private final DateUtils dateUtils;
    private final FundRecoupRepository fundRecoupRepository;
    private final VendingServiceRouteConfigRepository configRepository;
    private static final String SYSTEM_PRODUCT_TYPE = "flight";

    @Override
    public DefaultResponse searchFlight(SearchFlightRequest searchFlightRequest) {
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        try {
            boolean invalidDepartureDate = dateUtils.invalidDate(searchFlightRequest.getDepartureDate());
            if (invalidDepartureDate) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "Invalid departure date or format");
            }
            if (StringUtils.isNotBlank(searchFlightRequest.getReturnDate())) {
                boolean invalidReturnDate = dateUtils.invalidDate(searchFlightRequest.getReturnDate());
                if (invalidReturnDate) {
                    throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "Invalid return date or format");
                }
            }
            //Get product code from route config
            Optional<VendingServiceRouteConfig> routeConfig = configRepository.findFirstByIgnoreCaseSystemProductTypeAndActiveTrue(
                SYSTEM_PRODUCT_TYPE
            );
            if (routeConfig.isEmpty()) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "VendingServiceRouteConfig not found");
            }
            String productCode = routeConfig.get().getProductCode();
            searchFlightRequest.setProductCode(productCode);
            if (StringUtils.isBlank(productCode)) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "Product code is blank");
            }
            String processorId = getProcessorId(productCode);
            FlightProductAbstractVendingService flightProductVendingService = getFlightProductVendingService(processorId);
            log.info(">>> Calling get Flight Offer module");
            FlightResponse result = flightProductVendingService.searchFlight(searchFlightRequest);
            defaultResponse.setMessage(result.getMessage());
            defaultResponse.setStatus(result.getCode());
            defaultResponse.setData(result.getData());
            return defaultResponse;
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultResponse;
    }

    @Override
    public DefaultResponse validateFlight(ValidateFlight validateFlightRequest) {
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        try {
            //Get product code from route config
            Optional<VendingServiceRouteConfig> routeConfig = configRepository.findFirstByIgnoreCaseSystemProductTypeAndActiveTrue(
                SYSTEM_PRODUCT_TYPE
            );
            if (routeConfig.isEmpty()) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "VendingServiceRouteConfig not found");
            }
            String productCode = routeConfig.get().getProductCode();
            validateFlightRequest.setProductCode(productCode);
            String bookingCode = null;
            if (StringUtils.isBlank(productCode)) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "Product code is blank");
            }
            String processorId = getProcessorId(productCode);
            FlightProductAbstractVendingService flightProductVendingService = getFlightProductVendingService(processorId);
            log.info(">>> Calling confirmPrice module");
            ConfirmPriceResponse result = flightProductVendingService.confirmPrice(validateFlightRequest);
            if (result == null) {
                throw new BadRequestException(
                    TransactionStatus.TRANSACTION_FAILED.getMessage(),
                    TransactionStatus.TRANSACTION_FAILED.getCode()
                );
            }
            if (!result.getCode().equalsIgnoreCase(TransactionStatus.SUCCESS.getCode())) {
                defaultResponse.setStatus(result.getCode());
                defaultResponse.setMessage(result.getMessage());
                return defaultResponse;
            }

            bookingCode = ReferenceUtil.generateInternalReference();
            result.setBookingCode(bookingCode);
            flightProductVendingService.createBookingValidation(result, productCode);
            ValidateFlightData validateFlightData = new ValidateFlightData();
            validateFlightData.setBookingCode(bookingCode);
            validateFlightData.setPrice(result.getData().getAmount());
            validateFlightData.setPriceSummary(result.getData().getPriceSummary());
            defaultResponse.setMessage(result.getMessage());
            defaultResponse.setStatus(result.getCode());
            defaultResponse.setData(validateFlightData);

            return defaultResponse;
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultResponse;
    }

    @Override
    public DefaultResponse bookFlight(BookFlightRequest bookFlightRequest, MerchantDetailsDto merchantDetailsDto) {
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        try {
            if (transactionRepository.findByClientReference(bookFlightRequest.getPaymentIdentifier()).isPresent()) {
                throw new BadRequestException(
                    "transaction with client reference " + ResponseStatus.ALREADY_EXIST.getMessage(),
                    ResponseStatus.ALREADY_EXIST.getCode()
                );
            }
            Optional<BookingValidation> optionalBookingValidation = bookingValidationRepository.findFirstByBookingCode(
                bookFlightRequest.getBookingCode()
            );
            if (optionalBookingValidation.isEmpty()) {
                throw new BadRequestException("Booking not present ", ResponseStatus.BAD_REQUEST.getCode());
            }
            BookingValidation bookingValidation = optionalBookingValidation.get();
            String productCode = bookingValidation.getProductCode();
            String processorId = getProcessorId(productCode);
            Product product = getProduct(productCode);

            if (BookingStatus.BOOKINGCOMPLETED.equals(bookingValidation.getStatus())) {
                throw new BadRequestException("Flight already booked ", ResponseStatus.BAD_REQUEST.getCode());
            }

            validateBookingRequest(bookFlightRequest, bookingValidation);

            secureFunds(merchantDetailsDto, bookingValidation, bookFlightRequest);

            BookResponse bookResponse = book(bookFlightRequest, processorId, productCode);

            CommissionDTO commission = resolveCommission(merchantDetailsDto, bookResponse, bookingValidation);

            disBurseFunds(merchantDetailsDto, bookingValidation, bookResponse, bookFlightRequest, commission);

            FlightDto flightDto = buildFlightDto(merchantDetailsDto, bookingValidation, bookResponse, commission);

            finish(flightDto, processorId, product, bookResponse, bookFlightRequest);

            BookingResponse bookingResponse = new BookingResponse();
            bookingResponse.setPaymentIdentifier(bookFlightRequest.getPaymentIdentifier());
            bookingResponse.setBookingCode(bookFlightRequest.getBookingCode());

            defaultResponse.setMessage(bookResponse.getResult().getMessage());
            defaultResponse.setStatus(bookResponse.getResult().getCode());
            defaultResponse.setData(bookingResponse);
            return defaultResponse;
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultResponse;
    }

    private CommissionDTO resolveCommission(
        MerchantDetailsDto merchantDetailsDto,
        BookResponse bookResponse,
        BookingValidation bookingValidation
    ) {
        String vendProcessor = resolveFlightProcessor(bookResponse, bookingValidation);
        CommissionDTO commission = commissionUtil.resolveAmountWithCommission(
            bookingValidation.getProductCode(),
            bookingValidation.getAmount(),
            merchantDetailsDto,
            vendProcessor
        );
        log.info("commission is ....{}", commission);
        return commission;
    }

    private String resolveFlightProcessor(BookResponse bookResponse, BookingValidation bookingValidation) {
        String vendProcessor = bookResponse.isProcessedWithFallback()
            ? bookResponse.getFallbackProcessor()
            : getProcessorId(bookingValidation.getProductCode());
        log.info("vendProcessor is ....{}", vendProcessor);
        return vendProcessor;
    }

    private void finish(
        FlightDto flightDto,
        String processorId,
        Product product,
        BookResponse bookResponse,
        BookFlightRequest bookFlightRequest
    ) {
        FlightProductAbstractVendingService flightService = getFlightProductVendingService(processorId);
        flightService.createFlightTransaction(bookResponse.getResult(), flightDto, bookFlightRequest, product);
    }

    private FlightDto buildFlightDto(
        MerchantDetailsDto merchantDetailsDto,
        BookingValidation bookingValidation,
        BookResponse bookResponse,
        CommissionDTO commission
    ) {
        String subscriptionType = SubscriptionUtil.getSubscriptionType(merchantDetailsDto);
        FlightDto dto = new FlightDto();
        log.info("Inside buildFlightDto with bookingValidation ...{}", bookingValidation);
        dto.setUserId(merchantDetailsDto.getOrgId());
        dto.setInternalReference(bookingValidation.getBookingCode());
        dto.setProcessedWithFallback(bookResponse.isProcessedWithFallback());
        dto.setFallbackProcessorId(bookResponse.getFallbackProcessor());
        dto.setFallbackResponseMessage(bookResponse.getFallbackFailureMessage());
        dto.setMainResponseMessage(bookResponse.getMainFailureMessage());

        //set Merchant Info
        dto.setMerchantName(merchantDetailsDto.getRegisteredBusinessName());
        dto.setMerchantOrgId(merchantDetailsDto.getOrgId());
        dto.setAccountNumber(merchantDetailsDto.getAccountNumber());
        dto.setIpAddress(merchantDetailsDto.getRequestIp());
        dto.setBankCode(merchantDetailsDto.getBankCode());
        dto.setSubscriptionType(subscriptionType);
        dto.setTenantId(merchantDetailsDto.getTenantId());
        if (Objects.nonNull(commission)) {
            dto.setDiscountedAmount(commission.getDiscountedAmount());
            dto.setCommission(commission.getCommission());
        }
        dto.setServiceName("VENDING");
        dto.setService("VENDING");
        dto.setFundingType(subscriptionType);
        dto.setOrgId(merchantDetailsDto.getOrgId());
        log.info("Flight dto Inside buildFlightDto is ...{}", dto);
        return dto;
    }

    private void disBurseFunds(
        MerchantDetailsDto merchantDetailsDto,
        BookingValidation bookingValidation,
        BookResponse bookResponse,
        BookFlightRequest bookFlightRequest,
        CommissionDTO commission
    ) {
        String subscriptionType = SubscriptionUtil.getSubscriptionType(merchantDetailsDto);
        BookingResult bookingResult = bookResponse.getResult();
        if (TransactionStatus.SUCCESS.getCode().equals(bookingResult.getCode())) {
            bookingValidation.setStatus(BookingStatus.BOOKINGCOMPLETED);
            bookingValidation.setUpdatedAt(new Date());
            bookingValidationRepository.save(bookingValidation);

            if (subscriptionType.equalsIgnoreCase(String.valueOf(SubscriptionType.PREPAID))) {
                log.info("enter because of prepaid subscription");
                AccountDebitRequest accountDebitRequest = accountUtil.getAccountDebitRequest(
                    merchantDetailsDto,
                    commission.getDiscountedAmount(),
                    bookFlightRequest.getPaymentIdentifier()
                );
                String flightProcessor = resolveFlightProcessor(bookResponse, bookingValidation);
                CoreSDKResult releaseAndDebitResultMessage = accountUtil.releaseAndDebitFund(
                    accountDebitRequest,
                    VendProcessor.valueOf(flightProcessor.toUpperCase())
                );
                log.info("releaseAndDebitResultMessage is ....{}", releaseAndDebitResultMessage);
                if (releaseAndDebitResultMessage == CoreSDKResult.RELEASED) {
                    FundRecoup fundRecoup = accountUtil.getReleasedFundRecoup(bookFlightRequest);
                    fundRecoupRepository.save(fundRecoup);
                } else if (releaseAndDebitResultMessage == CoreSDKResult.FAILED) {
                    FundRecoup fundRecoup = accountUtil.getFailedFundRecoup(bookFlightRequest);
                    fundRecoupRepository.save(fundRecoup);
                }
            }
        } else if (TransactionStatus.TRANSACTION_FAILED.getCode().equals(bookingResult.getCode())) {
            if (subscriptionType.equalsIgnoreCase(String.valueOf(SubscriptionType.PREPAID))) {
                AccountDebitRequest accountReleaseRequest = accountUtil.getAccountDebitRequest(
                    merchantDetailsDto,
                    bookingValidation.getAmount(),
                    bookFlightRequest.getPaymentIdentifier()
                );
                CoreSDKResult releaseHeldFund = accountUtil.releaseHeldFund(accountReleaseRequest);
                log.info("releaseHeldFund on Merchant is => {}", releaseHeldFund);
                if (!CoreSDKResult.RELEASED.equals(releaseHeldFund)) {
                    FundRecoup fundRecoup = accountUtil.getHoldFundRecoup(bookFlightRequest);
                    fundRecoupRepository.save(fundRecoup);
                }
            }
        }
    }

    private void secureFunds(
        MerchantDetailsDto merchantDetailsDto,
        BookingValidation bookingValidation,
        BookFlightRequest bookFlightRequest
    ) {
        if (merchantDetailsDto == null) {
            throw new BadRequestException("Merchant not found", NOT_FOUND.getCode());
        }
        String subscriptionType = SubscriptionUtil.getSubscriptionType(merchantDetailsDto);
        if (Strings.isBlank(subscriptionType)) {
            throw new BadRequestException("Subscription type cannot be blank", NOT_FOUND.getCode());
        }

        if (subscriptionType.equalsIgnoreCase(String.valueOf(SubscriptionType.PREPAID))) {
            AccountDebitRequest accountHoldRequest = accountUtil.getAccountDebitRequest(
                merchantDetailsDto,
                bookingValidation.getAmount(),
                bookFlightRequest.getPaymentIdentifier()
            );
            accountUtil.holdOriginalTransactionAmount(accountHoldRequest);

            log.info(
                "Merchant with profileId {}, and account number {}, held successfully by amount: {}",
                merchantDetailsDto.getOrgId(),
                merchantDetailsDto.getAccountNumber(),
                bookingValidation.getAmount()
            );
        }
    }

    private void validateBookingRequest(BookFlightRequest bookFlightRequest, BookingValidation bookingValidation) {
        int requestedAdult = bookFlightRequest.getAdult().size();
        int confirmedAdult = bookingValidation.getNumberOfAdult();
        if (requestedAdult != confirmedAdult) {
            throw new BadRequestException("Number of Adults not matched ", ResponseStatus.BAD_REQUEST.getCode());
        }
        int requestedChildren = bookFlightRequest.getChildren().size();
        int confirmedChildren = bookingValidation.getNumberOfChildren();
        if (requestedChildren != confirmedChildren) {
            throw new BadRequestException("Number of Children not matched ", ResponseStatus.BAD_REQUEST.getCode());
        }
        int requestedInfant = bookFlightRequest.getInfant().size();
        int confirmedInfant = bookingValidation.getNumberOfInfant();
        if (requestedInfant != confirmedInfant) {
            throw new BadRequestException("Number of Infant not matched ", ResponseStatus.BAD_REQUEST.getCode());
        }
    }

    @Override
    public DefaultResponse cancelFlight(CancelFlightRequest cancelFlightRequest) {
        return null;
    }

    @Override
    public DefaultResponse getAirport() {
        log.info("Inside get airport");
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        try {
            //Get product code from route config
            Optional<VendingServiceRouteConfig> routeConfig = configRepository.findFirstByIgnoreCaseSystemProductTypeAndActiveTrue(
                SYSTEM_PRODUCT_TYPE
            );
            if (routeConfig.isEmpty()) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "VendingServiceRouteConfig not found");
            }
            String productCode = routeConfig.get().getProductCode();
            String processorId = getProcessorId(productCode);

            if (StringUtils.isBlank(processorId)) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "ProcessorId is blank");
            }
            FlightProductAbstractVendingService flightProductVendingService = getFlightProductVendingService(processorId);
            log.info(">>> Calling get Flight Offer module");
            AirportResponse result = flightProductVendingService.getAirport();
            defaultResponse.setMessage(result.getMessage());
            defaultResponse.setStatus(result.getCode());
            defaultResponse.setData(result.getData());
            return defaultResponse;
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultResponse;
    }

    @Override
    public DefaultResponse getAirlines(String processorId) {
        return null;
    }

    @Override
    public DefaultResponse convertCurrency(ConvertCurrencyRequest convertCurrencyRequest) {
        return null;
    }

    @Override
    public DefaultResponse getBookedFlightDetails(BookingDetailsRequest bookingDetailsRequest) {
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        try {
            String bookingCode = bookingDetailsRequest.getBookingCode();
            Optional<BookingValidation> optionalBookingValidation = bookingValidationRepository.findFirstByBookingCode(bookingCode);
            if (optionalBookingValidation.isEmpty()) {
                throw new BadRequestException("Booking not present ", ResponseStatus.BAD_REQUEST.getCode());
            }
            BookingValidation bookingValidation = optionalBookingValidation.get();
            String productCode = bookingValidation.getProductCode();
            String processorId = getProcessorId(productCode);
            FlightProductAbstractVendingService flightService = getFlightProductVendingService(processorId);
            BookingDetailsResponse response = flightService.getBookedFlightDetails(bookingDetailsRequest);

            defaultResponse.setMessage(response.getMessage());
            defaultResponse.setStatus(response.getCode());
            defaultResponse.setData(response.getData());
            return defaultResponse;
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultResponse;
    }

    @Override
    public DefaultResponse getWalletBalance(String processorId) {
        log.info(">>> Getting WalletBalance from processorId: {}", processorId);
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        try {
            validateProcessor(processorId);
            FlightProductAbstractVendingService flightService = getFlightProductVendingService(processorId);

            BalanceResponse response = flightService.getWalletBalance(processorId);
            if (response == null) {
                throw new NotFoundException(ResponseStatus.NOT_FOUND.getMessage(), ResponseStatus.NOT_FOUND.getCode());
            }
            defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
            defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
            defaultResponse.setData(response.getData());
            return defaultResponse;
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultResponse;
    }

    @Override
    public DefaultResponse createProcessorPackage(ProcessorPackageRequest request, String processorId) {
        log.info(">>> Creating ProcessorPackage from processorId: {}", processorId);
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        try {
            validateProcessor(processorId);
            Product product = productRepository
                .findByCode(request.getProductCode())
                .orElseThrow(() -> new NotFoundException("product " + ResponseStatus.NOT_FOUND.getMessage()));

            FlightProductAbstractVendingService flightService = getFlightProductVendingService(processorId);

            Object response = flightService.createVendingProcessorPackage(request);
            if (response == null) {
                throw new NotFoundException(ResponseStatus.NOT_FOUND.getMessage(), ResponseStatus.NOT_FOUND.getCode());
            }
            defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
            defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
            defaultResponse.setData(response);
            return defaultResponse;
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultResponse;
    }

    @Override
    public DefaultResponse getProcessorPackage(String processorId) {
        log.info(">>> Creating ProcessorPackage from processorId: {}", processorId);
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        try {
            validateProcessor(processorId);
            FlightProductAbstractVendingService flightService = getFlightProductVendingService(processorId);

            Object response = flightService.getVendingProcessorPackage();
            if (response == null) {
                throw new NotFoundException(ResponseStatus.NOT_FOUND.getMessage(), ResponseStatus.NOT_FOUND.getCode());
            }
            defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
            defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
            defaultResponse.setData(response);
            return defaultResponse;
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultResponse;
    }

    @Override
    public DefaultResponse createCabin(CabinClassRequest request, String processorId) {
        log.info(">>> Creating createCabin from processorId: {}", processorId);
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        try {
            validateProcessor(processorId);
            //            Product product = productRepository
            //                .findByCode(request.getProductCode())
            //                .orElseThrow(() -> new NotFoundException("product " + ResponseStatus.NOT_FOUND.getMessage()));

            FlightProductAbstractVendingService flightService = getFlightProductVendingService(processorId);

            Object response = flightService.createCabin(request);
            if (response == null) {
                throw new NotFoundException(ResponseStatus.NOT_FOUND.getMessage(), ResponseStatus.NOT_FOUND.getCode());
            }
            defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
            defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
            defaultResponse.setData(response);
            return defaultResponse;
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultResponse;
    }

    @Override
    public DefaultResponse fetchCabin() {
        log.info(">>> Creating createCabin from processorId:");
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        try {
            // validateProcessor(processorId);
            //Get product code from route config
            Optional<VendingServiceRouteConfig> routeConfig = configRepository.findFirstByIgnoreCaseSystemProductTypeAndActiveTrue(
                SYSTEM_PRODUCT_TYPE
            );
            if (routeConfig.isEmpty()) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "VendingServiceRouteConfig not found");
            }
            String productCode = routeConfig.get().getProductCode();
            if (StringUtils.isBlank(productCode)) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "Product code is blank");
            }
            String processorId = getProcessorId(productCode);
            FlightProductAbstractVendingService flightService = getFlightProductVendingService(processorId);

            Object response = flightService.fetchCabin();
            if (response == null) {
                throw new NotFoundException(ResponseStatus.NOT_FOUND.getMessage(), ResponseStatus.NOT_FOUND.getCode());
            }
            defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
            defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
            defaultResponse.setData(response);
            return defaultResponse;
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultResponse;
    }

    private String getProcessorId(String productCode) {
        log.info(">>> Getting getProcessorId from product: {}", productCode);
        String processorId = vendingServiceProcessorService.getProcessorId(productCode);
        if (processorId == null) {
            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }
        return processorId;
    }

    public Product getProduct(String code) {
        log.info(">>> Getting Product from code:{}", code);
        return productRepository
            .findByCode(code)
            .orElseThrow(() -> new NotFoundException("product " + ResponseStatus.NOT_FOUND.getMessage()));
    }

    private FlightProductAbstractVendingService getFlightProductVendingService(String processorId) {
        log.info(">>> Getting flightProductVendingService from processorId):{}", processorId);
        FlightProductAbstractVendingService serviceBean = flightProductVendingServiceDelegateBean.getDelegate(processorId);
        if (Objects.isNull(serviceBean)) {
            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }
        return serviceBean;
    }

    private BookResponse book(BookFlightRequest request, String productCode, String processorId) {
        FlightProductAbstractVendingService flightService = getFlightProductVendingService(processorId);

        BookingResult result = flightService.bookFlight(request);

        BookResponse performFlightResponse = new BookResponse();
        performFlightResponse.setTransactionStatus("SUCCESS");

        String fallbackProcessor = null;
        BookingResult fallbackResult = null;
        String fallbackFailureMessage = null;
        String mainFailureMessage = null;
        boolean processedWithFallback = false;
        String fallbackResponseCode = null;

        if ((result == null || TransactionStatus.TRANSACTION_FAILED.getCode().equals(result.getCode()))) {
            performFlightResponse.setTransactionStatus(TransactionStatus.TRANSACTION_FAILED.name());
            if (Boolean.TRUE.equals(result.isDoFallBack())) {
                fallbackProcessor = getFallbackProcessorId(productCode);
                log.info(" fallbackProcessor is ...{}", fallbackProcessor);
                if (fallbackProcessor != null) {
                    mainFailureMessage = result == null ? "unknown error" : result.getMessage();
                    flightService = getFlightProductVendingService(fallbackProcessor);
                    fallbackResult = flightService.bookFlight(request);
                    if (fallbackResult != null && !TransactionStatus.SUCCESS.getCode().equals(fallbackResult.getCode())) {
                        fallbackFailureMessage = fallbackResult.getMessage();
                        fallbackResponseCode = fallbackResult.getCode();
                        performFlightResponse.setTransactionStatus(TransactionStatus.TRANSACTION_FAILED.getMessage());
                    }

                    if (fallbackResult != null && TransactionStatus.SUCCESS.getCode().equals(fallbackResult.getCode())) {
                        processedWithFallback = true;
                        result = fallbackResult;
                    }
                }
            }
        }
        performFlightResponse.setResult(result);
        performFlightResponse.setFallbackResponseCode(fallbackResponseCode);
        performFlightResponse.setFallbackProcessor(fallbackProcessor);
        performFlightResponse.setFallbackResult(fallbackResult);
        performFlightResponse.setFallbackFailureMessage(fallbackFailureMessage);
        performFlightResponse.setMainFailureMessage(mainFailureMessage);
        performFlightResponse.setProcessedWithFallback(processedWithFallback);
        return performFlightResponse;
    }

    private String getProcessorId(TransactionRequest request) {
        log.info(">>> Getting getProcessorId from TransactionRequest:{}", request.getProductCode());
        String processorId = vendingServiceProcessorService.getProcessorId(request.getProductCode());
        if (processorId == null) {
            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }
        return processorId;
    }

    private String getFallbackProcessorId(String productCode) {
        return vendingServiceProcessorService.getFallbackProcessorId(productCode);
    }

    private boolean validateProcessor(String processorId) {
        Optional<Processors> processors = procRepository.findByProcessorId(processorId);
        return processors.isPresent();
    }
}
