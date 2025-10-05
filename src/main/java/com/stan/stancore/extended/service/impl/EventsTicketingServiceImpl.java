package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.dto.account.AccountDebitRequest;
import com.systemspecs.remita.dto.auth.MerchantDetailsDto;
import com.systemspecs.remita.enumeration.CoreSDKResult;
import com.systemspecs.remita.enumeration.VendProcessor;
import com.systemspecs.remita.vending.extended.dto.CommissionDTO;
import com.systemspecs.remita.vending.extended.dto.CompleteBookResponse;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.dto.response.LocalEventResponse;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.BadRequestException;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.exception.UnknownException;
import com.systemspecs.remita.vending.extended.service.EventsTicketingService;
import com.systemspecs.remita.vending.extended.util.*;
import com.systemspecs.remita.vending.vendingcommon.entity.FundRecoup;
import com.systemspecs.remita.vending.vendingcommon.entity.Product;
import com.systemspecs.remita.vending.vendingcommon.entity.VendingServiceRouteConfig;
import com.systemspecs.remita.vending.vendingcommon.enums.SubscriptionType;
import com.systemspecs.remita.vending.vendingcommon.enums.TransactionStatus;
import com.systemspecs.remita.vending.vendingcommon.eventsticketing.dto.request.*;
import com.systemspecs.remita.vending.vendingcommon.eventsticketing.dto.response.*;
import com.systemspecs.remita.vending.vendingcommon.eventsticketing.entity.EventListingBookingData;
import com.systemspecs.remita.vending.vendingcommon.eventsticketing.factory.EventProductVendingServiceDelegateBean;
import com.systemspecs.remita.vending.vendingcommon.eventsticketing.repository.EventBookingDataRepository;
import com.systemspecs.remita.vending.vendingcommon.eventsticketing.service.EventProductAbstractVendingService;
import com.systemspecs.remita.vending.vendingcommon.flight.repository.BookingValidationRepository;
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
public class EventsTicketingServiceImpl implements EventsTicketingService {

    private final ProductRepository productRepository;
    private final EventProductVendingServiceDelegateBean eventProductVendingServiceDelegateBean;
    private final VendingServiceProcessorService vendingServiceProcessorService;
    private final BookingValidationRepository bookingValidationRepository;
    private final AccountUtil accountUtil;
    private final CommissionUtil commissionUtil;
    private final TransactionRepository transactionRepository;
    private final FundRecoupRepository fundRecoupRepository;
    private final VendingServiceRouteConfigRepository configRepository;
    private static final String SYSTEM_PRODUCT_TYPE = "events";
    private final EventBookingDataRepository eventBookingDataRepository;

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

    @Override
    public DefaultResponse getListing(ListingParam listingParam) {
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
            ListingRequest listingRequest = new ListingRequest();
            listingRequest.setProductCode(productCode);
            if (StringUtils.isBlank(productCode)) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "Product code is blank");
            }
            String processorId = getProcessorId(productCode);
            EventProductAbstractVendingService eventProductVendingService = getEventProductVendingService(processorId);
            log.info(">>> Calling get Listing module");
            ListingResponse result = eventProductVendingService.getListing(listingRequest, listingParam);
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
    public DefaultResponse getSingleListing(SingleListingRequest singleListingRequest) {
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
            singleListingRequest.setProductCode(productCode);
            if (StringUtils.isBlank(productCode)) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "Product code is blank");
            }
            String processorId = getProcessorId(productCode);
            EventProductAbstractVendingService eventProductVendingService = getEventProductVendingService(processorId);
            log.info(">>> Calling get Listing module");
            SingleListingResponse result = eventProductVendingService.getSingleListing(singleListingRequest);
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
    public DefaultResponse createLocationBooking(
        LocationTicketBookingRequest locationTicketBookingRequest,
        MerchantDetailsDto merchantDetailsDto
    ) {
        return null;
    }

    @Override
    public DefaultResponse createEventBooking(EventTicketBookingRequest eventTicketBookingRequest) {
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
            eventTicketBookingRequest.setProductCode(productCode);
            if (StringUtils.isBlank(productCode)) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "Product code is blank");
            }
            String processorId = getProcessorId(productCode);
            EventProductAbstractVendingService eventProductVendingService = getEventProductVendingService(processorId);
            log.info(">>> Calling get Listing module");
            EventBookingResponse result = eventProductVendingService.createEventBooking(eventTicketBookingRequest);
            result.getData().setBookingId(ReferenceUtil.generateInternalReference());
            if (TransactionStatus.SUCCESS.getCode().equals(result.getCode())) {
                EventListingBookingData ventBookingData = getEventBookingData(result.getData());
                eventBookingDataRepository.save(ventBookingData);
                log.info(">>> Event booking data...{} saved successfully", result.getData());
            }
            defaultResponse.setMessage(result.getMessage());
            defaultResponse.setStatus(result.getCode());
            defaultResponse.setData(getLocalEventRespons(result.getData()));
            return defaultResponse;
        } catch (BadRequestException e) {
            defaultResponse.setStatus(e.getCode());
            defaultResponse.setMessage(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultResponse;
    }

    private EventListingBookingData getEventBookingData(EventBookingResponseData eventBookingResponseData) {
        EventListingBookingData eventListingBookingData = new EventListingBookingData();
        eventListingBookingData.setBookingId(eventBookingResponseData.getBookingId());
        eventListingBookingData.setTicketQuantity(eventBookingResponseData.getTicketQuantity());
        eventListingBookingData.setTicketAmount(eventBookingResponseData.getTicketAmount());
        eventListingBookingData.setAmountDue(eventBookingResponseData.getAmountDue());
        eventListingBookingData.setIsFree(eventBookingResponseData.getIsFree());
        eventListingBookingData.setSession(eventBookingResponseData.getSession());
        eventListingBookingData.setProductReference(eventBookingResponseData.getProductReference());
        eventListingBookingData.setBookingRef(eventBookingResponseData.getBookingRef());
        eventListingBookingData.setPaymentRef(eventBookingResponseData.getPaymentRef());
        eventListingBookingData.setCreatedAt(new Date());

        return eventListingBookingData;
    }

    private LocalEventResponse getLocalEventRespons(EventBookingResponseData eventBookingResponseData) {
        if (eventBookingResponseData == null) {
            return null;
        }
        LocalEventResponse localEventResponse = new LocalEventResponse();
        localEventResponse.setBookingId(eventBookingResponseData.getBookingId() != null ? eventBookingResponseData.getBookingId() : null);
        localEventResponse.setTicketQuantity(eventBookingResponseData.getTicketQuantity());
        localEventResponse.setTicketAmount(eventBookingResponseData.getTicketAmount());
        localEventResponse.setBookingRef(
            eventBookingResponseData.getBookingRef() != null ? eventBookingResponseData.getBookingRef() : null
        );
        localEventResponse.setCreatedAt(eventBookingResponseData.getCreatedAt() != null ? eventBookingResponseData.getCreatedAt() : null);
        localEventResponse.setExpiresAt(eventBookingResponseData.getExpiresAt() != null ? eventBookingResponseData.getExpiresAt() : null);

        return localEventResponse;
    }

    @Override
    public DefaultResponse completeEventBooking(
        CompleteEventBookingRequest completeEventBookingRequest,
        MerchantDetailsDto merchantDetailsDto
    ) {
        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(TransactionStatus.TRANSACTION_FAILED.getMessage());
        defaultResponse.setStatus(TransactionStatus.TRANSACTION_FAILED.getCode());
        try {
            if (transactionRepository.findByClientReference(completeEventBookingRequest.getPaymentIdentifier()).isPresent()) {
                throw new BadRequestException(
                    "transaction with client reference " + ResponseStatus.ALREADY_EXIST.getMessage(),
                    ResponseStatus.ALREADY_EXIST.getCode()
                );
            }
            Optional<EventListingBookingData> optionalEventBookingData = eventBookingDataRepository.findByBookingId(
                completeEventBookingRequest.getBookingId()
            );
            if (optionalEventBookingData.isEmpty()) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "EventListingBookingData not found");
            }

            Optional<VendingServiceRouteConfig> routeConfig = configRepository.findFirstByIgnoreCaseSystemProductTypeAndActiveTrue(
                SYSTEM_PRODUCT_TYPE
            );
            if (routeConfig.isEmpty()) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "VendingServiceRouteConfig not found");
            }
            String productCode = routeConfig.get().getProductCode();
            completeEventBookingRequest.setProductCode(productCode);
            EventListingBookingData eventListingBookingData = optionalEventBookingData.get();
            completeEventBookingRequest.setSession(eventListingBookingData.getSession());
            completeEventBookingRequest.setBookingRef(eventListingBookingData.getBookingRef());
            completeEventBookingRequest.setPaymentRef(eventListingBookingData.getPaymentRef());
            if (StringUtils.isBlank(productCode)) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "Product code is blank");
            }
            String processorId = getProcessorId(productCode);
            Product product = getProduct(productCode);

            final String internalRef = ReferenceUtil.generateInternalReference();

            secureFunds(merchantDetailsDto, completeEventBookingRequest);

            CompleteBookResponse bookResponse = book(completeEventBookingRequest, productCode, processorId);

            CommissionDTO commission = resolveCommission(merchantDetailsDto, bookResponse, completeEventBookingRequest);

            disBurseFunds(merchantDetailsDto, completeEventBookingRequest, bookResponse, commission);

            EventDto eventDto = buildEventDto(merchantDetailsDto, bookResponse, commission, internalRef);

            finish(eventDto, processorId, product, bookResponse, completeEventBookingRequest);

            CompleteEventBookingResponse bookingResponse = new CompleteEventBookingResponse();
            bookingResponse.setPaymentIdentifier(completeEventBookingRequest.getPaymentIdentifier());
            bookingResponse.setBookingCode(completeEventBookingRequest.getBookingRef());

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

    @Override
    public DefaultResponse getCategory() {
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
            CategoryRequest categoryRequest = new CategoryRequest();
            categoryRequest.setProductCode(productCode);
            if (StringUtils.isBlank(productCode)) {
                throw new BadRequestException(TransactionStatus.BAD_REQUEST.getCode(), "Product code is blank");
            }
            String processorId = getProcessorId(productCode);
            EventProductAbstractVendingService eventProductVendingService = getEventProductVendingService(processorId);
            log.info(">>> Calling get category module");
            CategoryResponse result = eventProductVendingService.getCategory(categoryRequest);
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

    private void secureFunds(MerchantDetailsDto merchantDetailsDto, CompleteEventBookingRequest completeEventBookingRequest) {
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
                completeEventBookingRequest.getAmount(),
                completeEventBookingRequest.getPaymentIdentifier()
            );
            accountUtil.holdOriginalTransactionAmount(accountHoldRequest);

            log.info(
                "Merchant with profileId {}, and account number {}, held successfully by amount: {}",
                merchantDetailsDto.getOrgId(),
                merchantDetailsDto.getAccountNumber(),
                completeEventBookingRequest.getAmount()
            );
        }
    }

    private CompleteBookResponse book(CompleteEventBookingRequest request, String productCode, String processorId) {
        EventProductAbstractVendingService eventService = getEventProductVendingService(processorId);

        CompleteEventBookingResult result = eventService.completeEventBooking(request);

        CompleteBookResponse performEventResponse = new CompleteBookResponse();
        performEventResponse.setTransactionStatus("SUCCESS");

        String fallbackProcessor = null;
        CompleteEventBookingResult fallbackResult = null;
        String fallbackFailureMessage = null;
        String mainFailureMessage = null;
        boolean processedWithFallback = false;
        String fallbackResponseCode = null;

        if ((result == null || TransactionStatus.TRANSACTION_FAILED.getCode().equals(result.getCode()))) {
            performEventResponse.setTransactionStatus(TransactionStatus.TRANSACTION_FAILED.name());
            if (Boolean.TRUE.equals(result.isDoFallBack())) {
                fallbackProcessor = getFallbackProcessorId(productCode);
                log.info(" fallbackProcessor is ...{}", fallbackProcessor);
                if (fallbackProcessor != null) {
                    mainFailureMessage = result == null ? "unknown error" : result.getMessage();
                    eventService = getEventProductVendingService(fallbackProcessor);
                    fallbackResult = eventService.completeEventBooking(request);
                    if (fallbackResult != null && !TransactionStatus.SUCCESS.getCode().equals(fallbackResult.getCode())) {
                        fallbackFailureMessage = fallbackResult.getMessage();
                        fallbackResponseCode = fallbackResult.getCode();
                        performEventResponse.setTransactionStatus(TransactionStatus.TRANSACTION_FAILED.getMessage());
                    }

                    if (fallbackResult != null && TransactionStatus.SUCCESS.getCode().equals(fallbackResult.getCode())) {
                        processedWithFallback = true;
                        result = fallbackResult;
                    }
                }
            }
        }
        performEventResponse.setResult(result);
        performEventResponse.setFallbackResponseCode(fallbackResponseCode);
        performEventResponse.setFallbackProcessor(fallbackProcessor);
        performEventResponse.setFallbackResult(fallbackResult);
        performEventResponse.setFallbackFailureMessage(fallbackFailureMessage);
        performEventResponse.setMainFailureMessage(mainFailureMessage);
        performEventResponse.setProcessedWithFallback(processedWithFallback);
        return performEventResponse;
    }

    private CommissionDTO resolveCommission(
        MerchantDetailsDto merchantDetailsDto,
        CompleteBookResponse bookResponse,
        CompleteEventBookingRequest completeEventBookingRequest
    ) {
        String vendProcessor = resolveEventProcessor(bookResponse, completeEventBookingRequest);
        CommissionDTO commission = commissionUtil.resolveAmountWithCommission(
            completeEventBookingRequest.getProductCode(),
            completeEventBookingRequest.getAmount(),
            merchantDetailsDto,
            vendProcessor
        );
        log.info("commission is ....{}", commission);
        return commission;
    }

    private void disBurseFunds(
        MerchantDetailsDto merchantDetailsDto,
        CompleteEventBookingRequest request,
        CompleteBookResponse bookResponse,
        CommissionDTO commission
    ) {
        String subscriptionType = SubscriptionUtil.getSubscriptionType(merchantDetailsDto);
        CompleteEventBookingResult bookingResult = bookResponse.getResult();
        if (TransactionStatus.SUCCESS.getCode().equals(bookingResult.getCode())) {
            if (subscriptionType.equalsIgnoreCase(String.valueOf(SubscriptionType.PREPAID))) {
                log.info("enter because of prepaid subscription");
                AccountDebitRequest accountDebitRequest = accountUtil.getAccountDebitRequest(
                    merchantDetailsDto,
                    commission.getDiscountedAmount(),
                    request.getPaymentIdentifier()
                );
                String eventProcessor = resolveEventProcessor(bookResponse, request);
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

    private EventDto buildEventDto(
        MerchantDetailsDto merchantDetailsDto,
        CompleteBookResponse bookResponse,
        CommissionDTO commission,
        String internalRef
    ) {
        String subscriptionType = SubscriptionUtil.getSubscriptionType(merchantDetailsDto);
        EventDto dto = new EventDto();
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
        log.info("Event dto Inside buildEventDto is ...{}", dto);
        return dto;
    }

    private void finish(
        EventDto eventDto,
        String processorId,
        Product product,
        CompleteBookResponse bookResponse,
        CompleteEventBookingRequest request
    ) {
        EventProductAbstractVendingService eventService = getEventProductVendingService(processorId);
        eventService.createEventTransaction(bookResponse.getResult(), eventDto, request, product);
    }

    private String resolveEventProcessor(CompleteBookResponse bookResponse, CompleteEventBookingRequest completeEventBookingRequest) {
        String vendProcessor = bookResponse.isProcessedWithFallback()
            ? bookResponse.getFallbackProcessor()
            : getProcessorId(completeEventBookingRequest.getProductCode());
        log.info("vendProcessor is ....{}", vendProcessor);
        return vendProcessor;
    }

    //    private String getProcessorId(TransactionRequest request) {
    //        log.info(">>> Getting getProcessorId from TransactionRequest:{}", request.getProductCode());
    //        String processorId = vendingServiceProcessorService.getProcessorId(request.getProductCode());
    //        if (processorId == null) {
    //            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
    //        }
    //        return processorId;
    //    }

    private String getFallbackProcessorId(String productCode) {
        return vendingServiceProcessorService.getFallbackProcessorId(productCode);
    }

    //    private boolean validateProcessor(String processorId) {
    //        Optional<Processors> processors = processorsRepository.findByProcessorId(processorId);
    //        return processors.isPresent();
    //    }

    private EventProductAbstractVendingService getEventProductVendingService(String processorId) {
        log.info(">>> Getting EventProductVendingService from processorId):{}", processorId);
        EventProductAbstractVendingService serviceBean = eventProductVendingServiceDelegateBean.getDelegate(processorId);
        if (Objects.isNull(serviceBean)) {
            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }
        return serviceBean;
    }
}
