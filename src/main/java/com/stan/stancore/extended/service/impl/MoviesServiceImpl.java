package com.stan.stancore.extended.service.impl;

import com.nimbusds.oauth2.sdk.util.StringUtils;
import com.systemspecs.remita.dto.account.AccountDebitRequest;
import com.systemspecs.remita.dto.auth.MerchantDetailsDto;
import com.systemspecs.remita.enumeration.CoreSDKResult;
import com.systemspecs.remita.enumeration.VendProcessor;
import com.systemspecs.remita.vending.extended.dto.CommissionDTO;
import com.systemspecs.remita.vending.extended.dto.CompleteMovieBookResponse;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.BadRequestException;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.exception.UnknownException;
import com.systemspecs.remita.vending.extended.service.MoviesService;
import com.systemspecs.remita.vending.extended.util.AccountUtil;
import com.systemspecs.remita.vending.extended.util.CommissionUtil;
import com.systemspecs.remita.vending.extended.util.ReferenceUtil;
import com.systemspecs.remita.vending.extended.util.SubscriptionUtil;
import com.systemspecs.remita.vending.vendingcommon.entity.FundRecoup;
import com.systemspecs.remita.vending.vendingcommon.entity.Product;
import com.systemspecs.remita.vending.vendingcommon.entity.VendingServiceRouteConfig;
import com.systemspecs.remita.vending.vendingcommon.enums.SubscriptionType;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import com.systemspecs.remita.vending.vendingcommon.eventsticketing.dto.response.CompleteEventBookingResponse;
import com.systemspecs.remita.vending.vendingcommon.movies.dto.request.*;
import com.systemspecs.remita.vending.vendingcommon.movies.dto.response.*;
import com.systemspecs.remita.vending.vendingcommon.movies.entity.MovieBookingData;
import com.systemspecs.remita.vending.vendingcommon.movies.factory.MoviesVendingServiceDelegateBean;
import com.systemspecs.remita.vending.vendingcommon.movies.repository.MovieBookingDataRepository;
import com.systemspecs.remita.vending.vendingcommon.movies.service.MoviesProductAbstractVendingService;
import com.systemspecs.remita.vending.vendingcommon.movies.utils.AmountUtil;
import com.systemspecs.remita.vending.vendingcommon.repository.FundRecoupRepository;
import com.systemspecs.remita.vending.vendingcommon.repository.ProductRepository;
import com.systemspecs.remita.vending.vendingcommon.repository.TransactionRepository;
import com.systemspecs.remita.vending.vendingcommon.repository.VendingServiceRouteConfigRepository;
import com.systemspecs.remita.vending.vendingcommon.service.VendingServiceProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import static com.systemspecs.remita.vending.extended.enums.ResponseStatus.INTERNAL_SERVER_ERROR;
import static com.systemspecs.remita.vending.extended.enums.ResponseStatus.NOT_FOUND;

@Slf4j
@RequiredArgsConstructor
@Service
public class MoviesServiceImpl implements MoviesService {

    private static final String SYSTEM_PRODUCT_TYPE = "MOVIES";
    private final VendingServiceRouteConfigRepository configRepository;
    private final VendingServiceProcessorService vendingServiceProcessorService;
    private final MoviesVendingServiceDelegateBean movies;
    private final MovieBookingDataRepository movieBookingDataRepository;
    private final TransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final AccountUtil accountUtil;
    private final CommissionUtil commissionUtil;
    private final FundRecoupRepository fundRecoupRepository;

    @Override
    public DefaultResponse getMovies(MovieParam movieParam) {
        log.info("MovieServiceImpl::getMovies with param {}", movieParam);
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
            GetAllMoviesRequest movieRequest = new GetAllMoviesRequest();
            movieRequest.setProductCode(productCode);
            if (org.apache.commons.lang.StringUtils.isBlank(productCode)) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "Product code is blank");
            }

            log.info(">>> Calling get movie module");
            MoviesProductAbstractVendingService service = resolveMoviesService();
            MovieResponse result = service.fetchMovies(movieRequest, movieParam);

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

    public DefaultResponse getSingleMovie(GetMovieByName nameRequest) {
        log.info("MovieServiceImpl::getSingleMovie with nameRequest {}", nameRequest);
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
            nameRequest.setProductCode(productCode);
            if (org.apache.commons.lang.StringUtils.isBlank(productCode)) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "Product code is blank");
            }

            log.info(">>> Calling get movie module");
            MoviesProductAbstractVendingService service = resolveMoviesService();
            SingleMoviesResponseDto result = service.fetchMoviesByName(nameRequest);

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
    public DefaultResponse getMovieShowTime(GetShowTimeRequest request) {
        log.info("Inside getMovieShowTime with request: ...{}", request);
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
            request.setProductCode(productCode);
            if (org.apache.commons.lang.StringUtils.isBlank(productCode)) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "Product code is blank");
            }

            log.info(">>> Calling getMovieShowTime module");
            MoviesProductAbstractVendingService service = resolveMoviesService();
            MovieShowtimeResponse result = service.fetchMoviesShowTimes(request);

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
    public DefaultResponse createMovieBooking(CreateMovieBookingRequest createMovieBookingRequest) {
        log.info("Inside createMovieBooking with request: ...{}", createMovieBookingRequest);
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        try {
            if (createMovieBookingRequest == null) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "Request not found");
            }
            if (createMovieBookingRequest.getTickets().isEmpty()) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "Ticket not found");
            }
            BigDecimal amount = createMovieBookingRequest.getTickets().get(0).getPrice();
            boolean validAmount = AmountUtil.validateAmount(amount);
            if (!validAmount) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "Invalid amount");
            }
            //Get product code from route config
            Optional<VendingServiceRouteConfig> routeConfig = configRepository.findFirstByIgnoreCaseSystemProductTypeAndActiveTrue(
                SYSTEM_PRODUCT_TYPE
            );
            if (routeConfig.isEmpty()) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "VendingServiceRouteConfig not found");
            }
            String productCode = routeConfig.get().getProductCode();
            createMovieBookingRequest.setProductCode(productCode);
            if (org.apache.commons.lang.StringUtils.isBlank(productCode)) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "Product code is blank");
            }
            MoviesProductAbstractVendingService service = resolveMoviesService();
            log.info(">>> Calling createMovieBooking module");
            CreateBookingsResponse result = service.createMovieBooking(createMovieBookingRequest);
            log.info("CreateBookingsResponse result ... before booking id {}", result);
            result.getData().setBookingId(ReferenceUtil.generateInternalReference());
            log.info("CreateBookingsResponse result ...after booking id {}", result);
            if (TransactionStatus.SUCCESS.getCode().equals(result.getCode())) {
                MovieBookingData movieBookingData = getMovieBookingData(result.getData());
                log.info(">>> movieBookingData ...{}", movieBookingData);
                if (movieBookingData != null) {
                    movieBookingDataRepository.save(movieBookingData);
                    log.info(">>> Movie booking data...{} saved successfully", result.getData());
                }
            }
            defaultResponse.setMessage(result.getMessage());
            defaultResponse.setStatus(result.getCode());
            defaultResponse.setData(getLocalMovieResponse(result.getData()));
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
    public DefaultResponse completeMovieBooking(
        CompleteMovieBookingRequest completeMovieBookingRequest,
        MerchantDetailsDto merchantDetailsDto
    ) {
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        try {
            if (transactionRepository.findByClientReference(completeMovieBookingRequest.getPaymentIdentifier()).isPresent()) {
                throw new BadRequestException(
                    "transaction with client reference " + ResponseStatus.ALREADY_EXIST.getMessage(),
                    ResponseStatus.ALREADY_EXIST.getCode()
                );
            }
            Optional<MovieBookingData> optionalMovieBookingData = movieBookingDataRepository.findByBookingId(
                completeMovieBookingRequest.getBookingId()
            );
            if (optionalMovieBookingData.isEmpty()) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "MovieBookingData not found");
            }

            Optional<VendingServiceRouteConfig> routeConfig = configRepository.findFirstByIgnoreCaseSystemProductTypeAndActiveTrue(
                SYSTEM_PRODUCT_TYPE
            );
            if (routeConfig.isEmpty()) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "VendingServiceRouteConfig not found");
            }
            String productCode = routeConfig.get().getProductCode();
            completeMovieBookingRequest.setProductCode(productCode);
            MovieBookingData movieBookingData = optionalMovieBookingData.get();
            completeMovieBookingRequest.setSession(movieBookingData.getSession());
            completeMovieBookingRequest.setBookingRef(movieBookingData.getBookingRef());
            completeMovieBookingRequest.setPaymentRef(movieBookingData.getPaymentRef());
            if (org.apache.commons.lang.StringUtils.isBlank(productCode)) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "Product code is blank");
            }
            String processorId = getProcessorId(productCode);
            Product product = getProduct(productCode);

            final String internalRef = ReferenceUtil.generateInternalReference();

            secureFunds(merchantDetailsDto, completeMovieBookingRequest);

            CompleteMovieBookResponse bookResponse = book(completeMovieBookingRequest, productCode, processorId);

            CommissionDTO commission = resolveCommission(merchantDetailsDto, bookResponse, completeMovieBookingRequest);

            disBurseFunds(merchantDetailsDto, completeMovieBookingRequest, bookResponse, commission);

            MovieDto movieDto = buildMovieDto(merchantDetailsDto, bookResponse, commission, internalRef);

            finish(movieDto, processorId, product, bookResponse, completeMovieBookingRequest);

            CompleteEventBookingResponse bookingResponse = new CompleteEventBookingResponse();
            bookingResponse.setPaymentIdentifier(completeMovieBookingRequest.getPaymentIdentifier());
            //bookingResponse.setBookingCode(completeMovieBookingRequest.getSession());
            bookingResponse.setBookingCode(bookResponse.getResult().getBookingCode());

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

    private MovieBookingData getMovieBookingData(MovieBookingResponseData movieBookingResponseData) {
        log.info("Inside getMovieBookingData with  movieBookingResponseData...{}", movieBookingResponseData);
        if (movieBookingResponseData != null) {
            MovieBookingData movieBookingData = new MovieBookingData();
            movieBookingData.setBookingId(movieBookingResponseData.getBookingId());
            movieBookingData.setTicketAmount(movieBookingResponseData.getTicketAmount());
            movieBookingData.setAmountDue(movieBookingResponseData.getAmountDue());
            movieBookingData.setIsFree(movieBookingResponseData.getIsFree());
            movieBookingData.setSession(movieBookingResponseData.getSession());
            movieBookingData.setProductReference(movieBookingResponseData.getProductReference());
            movieBookingData.setBookingRef(movieBookingResponseData.getBookingRef());
            movieBookingData.setPaymentRef(movieBookingResponseData.getPaymentRef());
            movieBookingData.setCreatedAt(new Date());
            movieBookingData.setProductCode(movieBookingResponseData.getProductCode());
            movieBookingData.setName(movieBookingResponseData.getName());
            movieBookingData.setEmail(movieBookingResponseData.getEmail());
            movieBookingData.setPhone(movieBookingResponseData.getPhone());
            movieBookingData.setMovieName(movieBookingResponseData.getMovieName());
            movieBookingData.setCinemaName(movieBookingResponseData.getCinemaName());
            log.info("Movie Booking Data: {}", movieBookingData);
            return movieBookingData;
        }
        return null;
    }

    private LocalMovieResponse getLocalMovieResponse(MovieBookingResponseData movieBookingResponseData) {
        log.info("Inside getLocalMovieResponse with movieBookingResponseData...{}", movieBookingResponseData);
        if (movieBookingResponseData == null) {
            return null;
        }
        LocalMovieResponse localMovieResponse = new LocalMovieResponse();
        localMovieResponse.setBookingId(movieBookingResponseData.getBookingId() != null ? movieBookingResponseData.getBookingId() : null);
        localMovieResponse.setTicketQuantity(movieBookingResponseData.getTicketQuantity());
        localMovieResponse.setTicketAmount(movieBookingResponseData.getTicketAmount());
        localMovieResponse.setBookingRef(
            movieBookingResponseData.getBookingRef() != null ? movieBookingResponseData.getBookingRef() : null
        );
        localMovieResponse.setCreatedAt(movieBookingResponseData.getCreatedAt() != null ? movieBookingResponseData.getCreatedAt() : null);
        localMovieResponse.setExpiresAt(movieBookingResponseData.getExpiresAt() != null ? movieBookingResponseData.getExpiresAt() : null);
        log.info("LocalMovie Response: {}", localMovieResponse);
        return localMovieResponse;
    }

    public Product getProduct(String code) {
        log.info(">>> Getting Product from code:{}", code);
        return productRepository
            .findByCode(code)
            .orElseThrow(() -> new NotFoundException("product " + ResponseStatus.NOT_FOUND.getMessage()));
    }

    private void secureFunds(MerchantDetailsDto merchantDetailsDto, CompleteMovieBookingRequest completeMovieBookingRequest) {
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
                completeMovieBookingRequest.getAmount(),
                completeMovieBookingRequest.getPaymentIdentifier()
            );
            accountUtil.holdOriginalTransactionAmount(accountHoldRequest);

            log.info(
                "Merchant with profileId {}, and account number {}, held successfully by amount: {}",
                merchantDetailsDto.getOrgId(),
                merchantDetailsDto.getAccountNumber(),
                completeMovieBookingRequest.getAmount()
            );
        }
    }

    private CompleteMovieBookResponse book(CompleteMovieBookingRequest request, String productCode, String processorId) {
        MoviesProductAbstractVendingService service = resolveMoviesService();

        CompleteMovieBookingResult result = service.completeMovieBooking(request);

        CompleteMovieBookResponse performMovieResponse = new CompleteMovieBookResponse();
        performMovieResponse.setTransactionStatus("SUCCESS");

        String fallbackProcessor = null;
        CompleteMovieBookingResult fallbackResult = null;
        String fallbackFailureMessage = null;
        String mainFailureMessage = null;
        boolean processedWithFallback = false;
        String fallbackResponseCode = null;

        if ((result == null || TransactionStatus.TRANSACTION_FAILED.getCode().equals(result.getCode()))) {
            performMovieResponse.setTransactionStatus(TransactionStatus.TRANSACTION_FAILED.name());
            if (Boolean.TRUE.equals(result.isDoFallBack())) {
                fallbackProcessor = getFallbackProcessorId(productCode);
                log.info(" fallbackProcessor is ...{}", fallbackProcessor);
                if (fallbackProcessor != null) {
                    mainFailureMessage = result == null ? "unknown error" : result.getMessage();
                    service = resolveFallbackMoviesService();
                    fallbackResult = service.completeMovieBooking(request);
                    if (fallbackResult != null && !TransactionStatus.SUCCESS.getCode().equals(fallbackResult.getCode())) {
                        fallbackFailureMessage = fallbackResult.getMessage();
                        fallbackResponseCode = fallbackResult.getCode();
                        performMovieResponse.setTransactionStatus(TransactionStatus.TRANSACTION_FAILED.getMessage());
                    }

                    if (fallbackResult != null && TransactionStatus.SUCCESS.getCode().equals(fallbackResult.getCode())) {
                        processedWithFallback = true;
                        result = fallbackResult;
                    }
                }
            }
        }
        performMovieResponse.setResult(result);
        performMovieResponse.setFallbackResponseCode(fallbackResponseCode);
        performMovieResponse.setFallbackProcessor(fallbackProcessor);
        performMovieResponse.setFallbackResult(fallbackResult);
        performMovieResponse.setFallbackFailureMessage(fallbackFailureMessage);
        performMovieResponse.setMainFailureMessage(mainFailureMessage);
        performMovieResponse.setProcessedWithFallback(processedWithFallback);
        return performMovieResponse;
    }

    private CommissionDTO resolveCommission(
        MerchantDetailsDto merchantDetailsDto,
        CompleteMovieBookResponse bookResponse,
        CompleteMovieBookingRequest completeMovieBookingRequest
    ) {
        String vendProcessor = resolveMovieProcessor(bookResponse, completeMovieBookingRequest);
        CommissionDTO commission = commissionUtil.resolveAmountWithCommission(
            completeMovieBookingRequest.getProductCode(),
            completeMovieBookingRequest.getAmount(),
            merchantDetailsDto,
            vendProcessor
        );
        log.info("commission is ....{}", commission);
        return commission;
    }

    private String resolveMovieProcessor(CompleteMovieBookResponse bookResponse, CompleteMovieBookingRequest completeMovieBookingRequest) {
        String vendProcessor = bookResponse.isProcessedWithFallback()
            ? bookResponse.getFallbackProcessor()
            : getProcessorId(completeMovieBookingRequest.getProductCode());
        log.info("vendProcessor is ....{}", vendProcessor);
        return vendProcessor;
    }

    private void disBurseFunds(
        MerchantDetailsDto merchantDetailsDto,
        CompleteMovieBookingRequest request,
        CompleteMovieBookResponse bookResponse,
        CommissionDTO commission
    ) {
        String subscriptionType = SubscriptionUtil.getSubscriptionType(merchantDetailsDto);
        CompleteMovieBookingResult bookingResult = bookResponse.getResult();
        if (TransactionStatus.SUCCESS.getCode().equals(bookingResult.getCode())) {
            if (subscriptionType.equalsIgnoreCase(String.valueOf(SubscriptionType.PREPAID))) {
                log.info("enter because of prepaid subscription");
                AccountDebitRequest accountDebitRequest = accountUtil.getAccountDebitRequest(
                    merchantDetailsDto,
                    commission.getDiscountedAmount(),
                    request.getPaymentIdentifier()
                );
                String eventProcessor = resolveMovieProcessor(bookResponse, request);
                CoreSDKResult releaseAndDebitResultMessage = accountUtil.releaseAndDebitFund(
                    accountDebitRequest,
                    VendProcessor.valueOf(eventProcessor.toUpperCase())
                );
                log.info("releaseAndDebitResultMessage is ....{}", releaseAndDebitResultMessage);
                if (releaseAndDebitResultMessage == CoreSDKResult.RELEASED) {
                    FundRecoup fundRecoup = accountUtil.getReleasedFundRecoup(request);
                    fundRecoupRepository.save(fundRecoup);
                } else if (releaseAndDebitResultMessage == CoreSDKResult.FAILED) {
                    FundRecoup fundRecoup = accountUtil.getFailedFundRecoup(request);
                    fundRecoupRepository.save(fundRecoup);
                }
            }
        } else if (TransactionStatus.TRANSACTION_FAILED.getCode().equals(bookingResult.getCode())) {
            if (subscriptionType.equalsIgnoreCase(String.valueOf(SubscriptionType.PREPAID))) {
                AccountDebitRequest accountReleaseRequest = accountUtil.getAccountDebitRequest(
                    merchantDetailsDto,
                    request.getAmount(),
                    request.getPaymentIdentifier()
                );
                CoreSDKResult releaseHeldFund = accountUtil.releaseHeldFund(accountReleaseRequest);
                log.info("releaseHeldFund on Merchant is => {}", releaseHeldFund);
                if (!CoreSDKResult.RELEASED.equals(releaseHeldFund)) {
                    FundRecoup fundRecoup = accountUtil.getHoldFundRecoup(request);
                    fundRecoupRepository.save(fundRecoup);
                }
            }
        }
    }

    private MovieDto buildMovieDto(
        MerchantDetailsDto merchantDetailsDto,
        CompleteMovieBookResponse bookResponse,
        CommissionDTO commission,
        String internalRef
    ) {
        String subscriptionType = SubscriptionUtil.getSubscriptionType(merchantDetailsDto);
        MovieDto dto = new MovieDto();
        dto.setUserId(merchantDetailsDto.getOrgId());
        dto.setInternalReference(internalRef);
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
        log.info("Movie dto Inside buildMovieDto is ...{}", dto);
        return dto;
    }

    private void finish(
        MovieDto movieDto,
        String processorId,
        Product product,
        CompleteMovieBookResponse bookResponse,
        CompleteMovieBookingRequest request
    ) {
        MoviesProductAbstractVendingService service = resolveMoviesService();
        service.createMovieTransaction(bookResponse.getResult(), movieDto, request, product);
    }

    private <T> DefaultResponse execute(
        String operation,
        TransactionStatus failureStatus,
        java.util.function.Function<MoviesProductAbstractVendingService, DefaultResponse> action
    ) {
        DefaultResponse defaultResponse = buildResponse(failureStatus, null);

        try {
            MoviesProductAbstractVendingService service = resolveMoviesService();
            log.info(">>> Executing {} with processorId={}", operation, service.getClass().getSimpleName());
            return action.apply(service);
        } catch (BadRequestException e) {
            log.error("Bad request in {}: {}", operation, e.getMessage());
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in {}: ", operation, e);
        }
        return defaultResponse;
    }

    private MoviesProductAbstractVendingService resolveMoviesService() {
        String productCode = getProductCode();
        String processorId = getProcessorId(productCode);
        MoviesProductAbstractVendingService serviceBean = movies.getDelegate(processorId);

        if (Objects.isNull(serviceBean)) {
            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }
        return serviceBean;
    }

    private MoviesProductAbstractVendingService resolveFallbackMoviesService() {
        String productCode = getProductCode();
        String fallbackProcessorId = getFallbackProcessorId(productCode);
        MoviesProductAbstractVendingService serviceBean = movies.getDelegate(fallbackProcessorId);

        if (Objects.isNull(serviceBean)) {
            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }
        return serviceBean;
    }

    private String getProductCode() {
        return configRepository
            .findFirstByIgnoreCaseSystemProductTypeAndActiveTrue(SYSTEM_PRODUCT_TYPE)
            .map(VendingServiceRouteConfig::getProductCode)
            .orElseThrow(() ->
                new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "VendingServiceRouteConfig not found for movies")
            );
    }

    private String getProcessorId(String productCode) {
        String processorId = vendingServiceProcessorService.getProcessorId(productCode);
        if (StringUtils.isBlank(processorId)) {
            throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "ProcessorId is blank");
        }
        return processorId;
    }

    private DefaultResponse buildResponse(TransactionStatus status, Object data) {
        DefaultResponse response = new DefaultResponse();
        response.setMessage(status.getMessage());
        response.setStatus(status.getCode());
        response.setData(data);
        return response;
    }

    private String getFallbackProcessorId(String productCode) {
        return vendingServiceProcessorService.getFallbackProcessorId(productCode);
    }
}
