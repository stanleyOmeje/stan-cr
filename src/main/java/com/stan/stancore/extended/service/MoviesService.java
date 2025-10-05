package com.stan.stancore.extended.service;

import com.systemspecs.remita.dto.auth.MerchantDetailsDto;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.vendingcommon.movies.dto.request.*;

import javax.validation.Valid;

public interface MoviesService {
    /**
     * Fetch all movies with pagination
     *
     * @return DefaultResponse containing movies list
     */

    DefaultResponse getMovies(MovieParam movieParam);

    /**
     * Fetch a single movie by its name
     *
     * @param name movie identifier (name)
     * @return DefaultResponse containing movie details
     */

    DefaultResponse getSingleMovie(GetMovieByName name);

    /**
     * Fetch movie show times for a given cinema
     *
     * @param request for cinema show time
     * @return DefaultResponse containing show times
     */
    DefaultResponse getMovieShowTime(GetShowTimeRequest request);

    DefaultResponse createMovieBooking(CreateMovieBookingRequest createMovieBookingRequest);

    DefaultResponse completeMovieBooking(
        @Valid CompleteMovieBookingRequest completeMovieBookingRequest,
        MerchantDetailsDto merchantDetailsDto
    );
}
