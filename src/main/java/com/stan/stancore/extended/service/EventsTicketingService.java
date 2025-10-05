package com.stan.stancore.extended.service;

import com.systemspecs.remita.dto.auth.MerchantDetailsDto;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.vendingcommon.eventsticketing.dto.request.*;

import javax.validation.Valid;

public interface EventsTicketingService {
    DefaultResponse getListing(ListingParam listingParam);
    DefaultResponse getSingleListing(SingleListingRequest singleListingRequest);
    DefaultResponse createLocationBooking(LocationTicketBookingRequest locationTicketBookingRequest, MerchantDetailsDto merchantDetailsDto);
    DefaultResponse createEventBooking(EventTicketBookingRequest eventTicketBookingRequest);

    DefaultResponse completeEventBooking(
        @Valid CompleteEventBookingRequest completeEventBookingRequest,
        MerchantDetailsDto merchantDetailsDto
    );

    DefaultResponse getCategory();
}
