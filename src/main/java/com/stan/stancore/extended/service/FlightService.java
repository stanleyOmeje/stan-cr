package com.stan.stancore.extended.service;

import com.systemspecs.remita.dto.auth.MerchantDetailsDto;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.vendingcommon.dto.request.ProcessorPackageRequest;
import com.systemspecs.remita.vending.vendingcommon.flight.dto.request.*;

import javax.validation.Valid;

public interface FlightService {
    DefaultResponse searchFlight(SearchFlightRequest searchFlightRequest);
    DefaultResponse validateFlight(ValidateFlight validateFlightRequest);
    DefaultResponse bookFlight(BookFlightRequest bookFlightRequest, MerchantDetailsDto merchantDetailsDto);
    DefaultResponse cancelFlight(CancelFlightRequest cancelFlightRequest);
    DefaultResponse getAirport();
    DefaultResponse getAirlines(String processorId);
    DefaultResponse convertCurrency(ConvertCurrencyRequest convertCurrencyRequest);
    DefaultResponse getBookedFlightDetails(@Valid BookingDetailsRequest bookingDetailsRequest);
    DefaultResponse getWalletBalance(String processorId);
    DefaultResponse createProcessorPackage(ProcessorPackageRequest request, String processorId);
    DefaultResponse getProcessorPackage(String processorId);

    DefaultResponse createCabin(@Valid CabinClassRequest request, String processorId);

    DefaultResponse fetchCabin();
}
