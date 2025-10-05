package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.dtonemodule.dto.response.CountryDto;
import com.systemspecs.remita.vending.dtonemodule.entity.Countries;
import com.systemspecs.remita.vending.dtonemodule.repository.CountryRepository;
import com.systemspecs.remita.vending.dtonemodule.service.DtOneVendingWebService;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.CountryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CountryServiceImpl implements CountryService {

    private final CountryRepository countryRepository;
    private final DtOneVendingWebService vendingWebService;

    @Override
    public ResponseEntity<DefaultResponse> fetchCountries(int page, int perPage) {
        log.info("fetchCountries  page {} and  perPage {} ", page, perPage);
        Page<Countries> countriesPage = getCountriesWithPagination(page, perPage);
        List<Countries> countries = countriesPage.getContent();
        if (countries.isEmpty()) {
            List<CountryDto> countryResponse = vendingWebService.getCountries(page, perPage);
            log.info("fetchCountries  response {} ", countryResponse);
            DefaultResponse defaultResponse = new DefaultResponse();
            defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
            defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
            defaultResponse.setData(countryResponse);

            countryRepository.saveAll(mapToCountries(countryResponse));

            return ResponseEntity.ok(defaultResponse);
        } else {
            return ResponseEntity.ok(
                DefaultResponse.builder().data(mapToCountryDtos(countries)).message("Countries fetched successfully").build()
            );
        }
    }

    private List<Countries> mapToCountries(List<CountryDto> countryDtos) {
        return countryDtos
            .stream()
            .map(countryDto -> {
                Countries countries = new Countries();
                countries.setName(countryDto.getName());
                countries.setIsoCode(countryDto.getIsoCode());
                return countries;
            })
            .collect(Collectors.toList());
    }

    private List<CountryDto> mapToCountryDtos(List<Countries> countries) {
        return countries
            .stream()
            .map(countries1 -> {
                CountryDto countryDto = new CountryDto();
                countryDto.setName(countries1.getName());
                countryDto.setIsoCode(countries1.getIsoCode());
                return countryDto;
            })
            .collect(Collectors.toList());
    }

    private Page<Countries> getCountriesWithPagination(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page, pageSize);
        return countryRepository.findAll(pageable);
    }

    @Override
    public ResponseEntity<DefaultResponse> fetchCountryByIsoCode(String isoCode) {
        log.info(">>>Fetching Country by IsoCode {}", isoCode);
        Optional<Countries> countries = countryRepository.findByIsoCode(isoCode);
        if (countries.isEmpty()) {
            throw new NotFoundException("Country not found");
        }
        List<CountryDto> countryDtos = new ArrayList<>();
        countryDtos.add(new CountryDto(countries.get().getName(), countries.get().getIsoCode()));
        DefaultResponse countryResponse = new DefaultResponse(
            ResponseStatus.SUCCESS.getMessage(),
            ResponseStatus.SUCCESS.getCode(),
            countryDtos
        );

        return new ResponseEntity<>(countryResponse, HttpStatus.OK);
    }
}
