package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.extended.dto.request.CreateProviderRequest;
import com.systemspecs.remita.vending.extended.dto.request.DisplayProvider;
import com.systemspecs.remita.vending.extended.dto.request.ServiceMapper.ProviderMapper;
import com.systemspecs.remita.vending.extended.dto.request.UpdateProviderRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.AlreadyExistException;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.ProviderService;
import com.systemspecs.remita.vending.vendingcommon.dto.request.ProviderPage;
import com.systemspecs.remita.vending.vendingcommon.dto.request.ProviderSearchCriteria;
import com.systemspecs.remita.vending.vendingcommon.entity.Category;
import com.systemspecs.remita.vending.vendingcommon.entity.Provider;
import com.systemspecs.remita.vending.vendingcommon.repository.CategoryRepository;
import com.systemspecs.remita.vending.vendingcommon.repository.ProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProviderServiceImp implements ProviderService {

    private final ProviderRepository providerRepository;
    private static final String PROVIDER = "Provider";
    private static final String CATEGORY = "Category";
    private final ProviderQueryService queryService;
    private final ProviderMapper providerMapper;
    private final CategoryRepository categoryRepository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public ResponseEntity<DefaultResponse> createProvider(CreateProviderRequest request) {
        log.info(">>> Creating Provider with request: {}", request);
        if (request.getCategoryList().isEmpty()) {
            throw new NotFoundException(CATEGORY + ResponseStatus.NOT_FOUND.getMessage());
        }
        String code = request.getCode().replaceAll("\\s+", "-");
        Optional<Provider> providerCheck = providerRepository.findByCode(code);
        log.info("providerCheck...{}", providerCheck);
        if (providerCheck.isPresent()) {
            throw new AlreadyExistException(PROVIDER + " " + ResponseStatus.ALREADY_EXIST.getMessage());
        }
        Category category = null;
        List<Category> categoryList = new ArrayList<>();
        for (long id : request.getCategoryList()) {
            category = categoryRepository.findById(id).get();
            if (category == null) {
                throw new NotFoundException(CATEGORY + ResponseStatus.NOT_FOUND.getMessage());
            }
            categoryList.add(category);
        }
        Provider provider = new Provider();
        provider.setName(request.getName());
        provider.setCode(code);
        provider.setLogoUrl(request.getLogoUrl());
        provider.setDescription(request.getDescription());

        provider.setCreatedAt(new Date());
        provider = providerRepository.save(provider);

        provider.getCategory().addAll(categoryList);
        providerRepository.save(provider);
        DefaultResponse defaultResponse = new DefaultResponse(
            ResponseStatus.SUCCESS.getMessage(),
            ResponseStatus.SUCCESS.getCode(),
            provider
        );
        return new ResponseEntity<>(defaultResponse, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<DefaultResponse> updateProvider(UpdateProviderRequest request, String code) {
        log.info(">>> Updating Provider with request: {} and code: {}", request, code);
        Provider providerToUpdate = providerRepository.findByCode(code).stream().findFirst().orElse(null);
        if (providerToUpdate == null) {
            throw new NotFoundException(PROVIDER + ResponseStatus.NOT_FOUND.getMessage());
        }
        Category category = null;
        List<Category> categoryList = new ArrayList<>();
        if (request.getCategoryList() != null) {
            for (long id : request.getCategoryList()) {
                log.info("inside for loop for categoryList");
                category = categoryRepository.findById(id).get();
                if (category == null) {
                    throw new NotFoundException(CATEGORY + ResponseStatus.NOT_FOUND.getMessage());
                }
                categoryList.add(category);
            }
        }
        providerToUpdate.setName(request.getName());
        providerToUpdate.setDescription(request.getDescription());
        providerToUpdate.setLogoUrl(request.getLogoUrl());
        providerToUpdate.setUpdatedAt(new Date());

        providerToUpdate = providerRepository.save(providerToUpdate);
        providerToUpdate.getCategory().addAll(categoryList);
        providerRepository.save(providerToUpdate);

        DefaultResponse defaultResponse = new DefaultResponse(
            ResponseStatus.SUCCESS.getMessage(),
            ResponseStatus.SUCCESS.getCode(),
            providerToUpdate
        );
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DefaultResponse> fetchAllProviderWithFilterByAdmin(
        String code,
        String name,
        String description,
        int page,
        int pageSize
    ) {
        log.info(">>>>Fetching all Providers using filter");
        ProviderPage providerPage = new ProviderPage();
        providerPage.setPageNo(page);
        providerPage.setPageSize(pageSize);
        ProviderSearchCriteria searchCriteria = getSearchCriteria(code, name, description);
        Page<Provider> providers = queryService.getAllProviderWithFilter(providerPage, searchCriteria);
        DefaultResponse defaultResponse = new DefaultResponse();

        if (providerPage.getPageSize() > 50) {
            defaultResponse.setMessage("Maximum page size exceeded");
            defaultResponse.setStatus(ResponseStatus.FAILED_REQUIREMENT.getCode());
        }
        Map<String, Object> map = new HashMap<>();
        map.put("totalPage", providers.getTotalPages());
        map.put("totalContent", providers.getTotalElements());
        map.put("items", providers.getContent());
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setData(map);

        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DefaultResponse> fetchAllProviderWithFilter(
        String code,
        String name,
        String description,
        int page,
        int pageSize
    ) {
        log.info(">>>>Fetching all Providers using filter");
        ProviderPage providerPage = new ProviderPage();
        providerPage.setPageNo(page);
        providerPage.setPageSize(pageSize);
        ProviderSearchCriteria searchCriteria = getSearchCriteria(code, name, description);
        Page<Provider> providers = queryService.getAllProviderWithFilter(providerPage, searchCriteria);
        DefaultResponse defaultResponse = new DefaultResponse();

        if (providerPage.getPageSize() > 50) {
            defaultResponse.setMessage("Maximum page size exceeded");
            defaultResponse.setStatus(ResponseStatus.FAILED_REQUIREMENT.getCode());
        }

        Map<String, Object> map = new HashMap<>();
        map.put("totalPage", providers.getTotalPages());
        map.put("totalContent", providers.getTotalElements());
        List<DisplayProvider> displayProviderList = providers
            .getContent()
            .stream()
            .map(provider -> {
                DisplayProvider displayProvider = providerMapper.mapToDisplayProvider(provider);
                return displayProvider;
            })
            .collect(Collectors.toList());

        map.put("items", displayProviderList);
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setData(map);

        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    private ProviderSearchCriteria getSearchCriteria(String code, String name, String description) {
        ProviderSearchCriteria searchCriteria = new ProviderSearchCriteria();
        searchCriteria.setCode(code);
        searchCriteria.setName(name);
        searchCriteria.setDescription(description);
        return searchCriteria;
    }

    @Override
    public ResponseEntity<DefaultResponse> fetchProviderByCategory(String category, Pageable pageable) {
        log.info(">>> Fetching Provider by category");
        Category categories = categoryRepository
            .findById(Long.parseLong(category))
            .orElseThrow(() -> new NotFoundException(CATEGORY + ResponseStatus.NOT_FOUND.getMessage()));
        List<Provider> provider = providerRepository.findProviderByCategory(Long.parseLong(category));
        log.info("List<Provider>...{}", provider);
        if (Objects.isNull(provider)) {
            throw new NotFoundException(PROVIDER + ResponseStatus.NOT_FOUND.getMessage());
        }

        DefaultResponse defaultResponse = new DefaultResponse(
            ResponseStatus.SUCCESS.getMessage(),
            ResponseStatus.SUCCESS.getCode(),
            provider
        );
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }
}
