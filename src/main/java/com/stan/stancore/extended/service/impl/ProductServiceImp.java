package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.extended.dto.request.CreateProductRequest;
import com.systemspecs.remita.vending.extended.dto.request.DisplayProduct;
import com.systemspecs.remita.vending.extended.dto.request.ServiceMapper.ProductMapper;
import com.systemspecs.remita.vending.extended.dto.request.UpdateProductRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.AlreadyExistException;
import com.systemspecs.remita.vending.extended.exception.BadRequestException;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.exception.UnknownException;
import com.systemspecs.remita.vending.extended.service.ProductService;
import com.systemspecs.remita.vending.vendingcommon.dto.request.ProductPage;
import com.systemspecs.remita.vending.vendingcommon.dto.request.ProductSearchCriteria;
import com.systemspecs.remita.vending.vendingcommon.dto.response.PriceResponse;
import com.systemspecs.remita.vending.vendingcommon.entity.Category;
import com.systemspecs.remita.vending.vendingcommon.entity.FeeMapping;
import com.systemspecs.remita.vending.vendingcommon.entity.Product;
import com.systemspecs.remita.vending.vendingcommon.enums.ProductType;
import com.systemspecs.remita.vending.vendingcommon.factory.VendingServiceDelegateBean;
import com.systemspecs.remita.vending.vendingcommon.repository.CategoryRepository;
import com.systemspecs.remita.vending.vendingcommon.repository.FeeMappingRepository;
import com.systemspecs.remita.vending.vendingcommon.repository.ProductRepository;
import com.systemspecs.remita.vending.vendingcommon.service.AbstractVendingService;
import com.systemspecs.remita.vending.vendingcommon.service.VendingServiceProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Tuple;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static com.systemspecs.remita.vending.extended.enums.ResponseStatus.INTERNAL_SERVER_ERROR;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImp implements ProductService {

    private final ProductRepository productRepository;

    private final CategoryRepository categoryRepository;

    private final FeeMappingRepository feeMappingRepository;

    private final ProductMapper productMapper;

    private final ProductQueryService queryService;

    private final VendingServiceDelegateBean vendingServiceDelegateBean;

    private final VendingServiceProcessorService vendingServiceProcessorService;
    private static final String CATEGORY = "category ";
    private static final String PRODUCT = "product ";

    @Override
    public ResponseEntity<DefaultResponse> createProduct(CreateProductRequest createProductRequest) {
        log.info(">>> Creating Product with request: {}", createProductRequest);
        String code = createProductRequest.getCode().replaceAll("\\s+", "-");
        Optional<Product> serviceCheck = productRepository.findByCode(code);

        log.info(">>> Checking if Product already exist:");
        if (serviceCheck.isPresent()) {
            throw new AlreadyExistException(PRODUCT + " " + ResponseStatus.ALREADY_EXIST.getMessage());
        }
        Category category = categoryRepository
            .findByCode(createProductRequest.getCategoryCode())
            .orElseThrow(() -> new NotFoundException(CATEGORY + ResponseStatus.NOT_FOUND.getMessage()));

        validateCommissionCreationForProduct(createProductRequest);

        Product product = productMapper.mapToEntity(createProductRequest, category);
        product = productRepository.save(product);
        product.setFee(createFeeMapping(createProductRequest, product));

        DefaultResponse defaultResponse = new DefaultResponse(
            ResponseStatus.SUCCESS.getMessage(),
            ResponseStatus.SUCCESS.getCode(),
            product
        );
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    private void validateCommissionCreationForProduct(CreateProductRequest request) {
        log.info("{-} Validating for update commission with request{}", request);
        validateFixedCommission(request);
        validatePercentageCommission(request);
    }

    private void validateFixedCommission(CreateProductRequest request) {
        if (Boolean.TRUE.equals(request.getIsFixedCommission()) && (isZero(request.getFixedCommission()))) {
            throw new BadRequestException(
                "Fixed Commission amount can not be zero or null when isFixedCommission is true",
                ResponseStatus.FAILED_REQUIREMENT.getCode()
            );
        }
    }

    private void validatePercentageCommission(CreateProductRequest request) {
        if (
            Boolean.FALSE.equals(request.getIsFixedCommission()) &&
            (isZero(request.getPercentageCommission()) || StringUtils.isBlank(request.getPercentageCommission()))
        ) {
            throw new BadRequestException(
                "Percentage commission for product should not be zero or blank when isFixedCommission is false",
                ResponseStatus.FAILED_REQUIREMENT.getCode()
            );
        }
        if (!StringUtils.isBlank(request.getPercentageCommission()) && !isZero(request.getPercentageCommission())) {
            validatePercentageRange(request);
        }
    }

    private void validatePercentageRange(CreateProductRequest request) {
        BigDecimal percentageMinCap = request.getPercentageMinCap();
        BigDecimal percentageMaxCap = request.getPercentageMaxCap();

        if (percentageMinCap == null && percentageMaxCap == null) {
            return;
        }
        if (percentageMinCap == null || percentageMaxCap == null) {
            throw new BadRequestException(
                "If one percentage is specified, both PercentageMin and PercentageMax must be provided",
                ResponseStatus.INVALID_PARAMETER.getCode()
            );
        }

        if (percentageMinCap.compareTo(percentageMaxCap) > 0) {
            throw new BadRequestException(
                " Invalid percentage range: PercentageMinCap (" +
                percentageMinCap +
                "%) is greater than PercentageMaxCap (" +
                percentageMaxCap +
                "%)",
                ResponseStatus.FAILED_REQUIREMENT.getCode()
            );
        }

        if (percentageMinCap.compareTo(percentageMaxCap) == 0) {
            throw new BadRequestException(
                "PercentageMin and PercentageMax cannot be equal: PercentageMin (" +
                percentageMinCap +
                "%) and PercentageMax (" +
                percentageMaxCap +
                "%)",
                ResponseStatus.FAILED_REQUIREMENT.getCode()
            );
        }
    }

    private FeeMapping createFeeMapping(CreateProductRequest createProductRequest, Product product) {
        Optional<FeeMapping> optionalFeeMapping = feeMappingRepository.findFirstByProductCode(product.getCode());
        FeeMapping feeMapping = new FeeMapping();
        log.info(">>> Checking if feeMapping already exist:");
        if (optionalFeeMapping.isPresent()) {
            feeMapping = optionalFeeMapping.get();
        }

        feeMapping.setCreatedAt(new Date());
        feeMapping.setFeeType(createProductRequest.getFeeType());
        feeMapping.setAmount(createProductRequest.getAmount());
        feeMapping.setProduct(product);
        return feeMappingRepository.save(feeMapping);
    }

    private boolean isZero(String str) {
        if (str == null || str.trim().isEmpty()) {
            return true;
        }

        String trimmed = str.trim();

        if (trimmed.equals("0")) {
            return true;
        }

        try {
            double value = Double.parseDouble(trimmed);
            return value == 0.0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isZero(BigDecimal value) {
        if (value == null) {
            return true;
        }
        return value.compareTo(BigDecimal.ZERO) == 0;
    }

    @Override
    public ResponseEntity<DefaultResponse> updateProduct(UpdateProductRequest updateProductRequest, String code) {
        log.info(">>> Inside Update Products with request: {} and code: {}", updateProductRequest, code);
        Product product = productRepository
            .findByCode(code)
            .orElseThrow(() -> new NotFoundException(PRODUCT + ResponseStatus.NOT_FOUND.getMessage()));
        Category category = categoryRepository
            .findByCode(updateProductRequest.getCategoryCode())
            .orElseThrow(() -> new NotFoundException(CATEGORY + ResponseStatus.NOT_FOUND.getMessage()));

        validateCommissionUpdateForProduct(updateProductRequest);

        product.setName(updateProductRequest.getName());

        product.setDescription(updateProductRequest.getDescription());
        product.setApplyCommission(updateProductRequest.getApplyCommission());
        product.setPercentageCommission(updateProductRequest.getPercentageCommission());
        product.setIsFixedCommission(updateProductRequest.getIsFixedCommission());
        product.setFixedCommission(updateProductRequest.getFixedCommission());
        product.setUpdatedAt(new Date());
        product.setCategory(category);
        product.setFee(updateFeeMapping(updateProductRequest, product));
        product.setPercentageMaxCap(updateProductRequest.getPercentageMaxCap());
        product.setPercentageMinCap(updateProductRequest.getPercentageMinCap());
        product.setProvider(updateProductRequest.getProvider());
        product.setActive(updateProductRequest.getActive());
        productRepository.save(product);
        DefaultResponse defaultResponse = new DefaultResponse(
            ResponseStatus.SUCCESS.getMessage(),
            ResponseStatus.SUCCESS.getCode(),
            updateProductRequest
        );
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    private void validateCommissionUpdateForProduct(UpdateProductRequest request) {
        log.info("{-} Validating for update commission for product with request{}", request);

        validateFixedCommission(request);
        validatePercentageCommission(request);
    }

    private void validateFixedCommission(UpdateProductRequest request) {
        if (Boolean.TRUE.equals(request.getIsFixedCommission()) && (isZero(request.getFixedCommission()))) {
            throw new BadRequestException(
                "Fixed Commission amount can not be zero or null when isFixed commission is true",
                ResponseStatus.FAILED_REQUIREMENT.getCode()
            );
        }
    }

    private void validatePercentageCommission(UpdateProductRequest request) {
        if (
            Boolean.FALSE.equals(request.getIsFixedCommission()) &&
            (isZero(request.getPercentageCommission()) || StringUtils.isBlank(request.getPercentageCommission()))
        ) {
            throw new BadRequestException(
                "Percentage commission for product can not be zero or blank when isFixedCommission is false",
                ResponseStatus.FAILED_REQUIREMENT.getCode()
            );
        }
        if (!StringUtils.isBlank(request.getPercentageCommission()) && !isZero(request.getPercentageCommission())) {
            validatePercentageRange(request);
        }
    }

    private void validatePercentageRange(UpdateProductRequest updateRequest) {
        BigDecimal percentageMinCap = updateRequest.getPercentageMinCap();
        BigDecimal percentageMaxCap = updateRequest.getPercentageMaxCap();

        if (percentageMinCap == null && percentageMaxCap == null) {
            return;
        }

        if (percentageMinCap == null || percentageMaxCap == null) {
            throw new BadRequestException(
                "If one percentage is specified, both PercentageMin and PercentageMax must be provided ",
                ResponseStatus.INVALID_PARAMETER.getCode()
            );
        }

        if (percentageMinCap.compareTo(percentageMaxCap) > 0) {
            throw new BadRequestException(
                "Invalid percentage range: PercentageMinCap (" +
                percentageMinCap +
                "%) is greater than PercentageMaxCap (" +
                percentageMaxCap +
                "%)",
                ResponseStatus.FAILED_REQUIREMENT.getCode()
            );
        }

        if (percentageMinCap.compareTo(percentageMaxCap) == 0) {
            throw new BadRequestException(
                "PercentageMin and PercentageMax cannot be equal: PercentageMin (" +
                percentageMinCap +
                "%) and PercentageMax (" +
                percentageMaxCap +
                "%)",
                ResponseStatus.FAILED_REQUIREMENT.getCode()
            );
        }
    }

    private FeeMapping updateFeeMapping(UpdateProductRequest updateProductRequest, Product product) {
        log.info(">>> inside update feeMapping with request...{} and product...{}:", updateProductRequest, product);
        Optional<FeeMapping> optionalFeeMapping = feeMappingRepository.findFirstByProductCode(product.getCode());
        FeeMapping feeMapping;
        log.info(">>> Checking if feeMapping already exist:");
        if (optionalFeeMapping.isEmpty()) {
            throw new NotFoundException("feeMapping " + ResponseStatus.NOT_FOUND.getMessage());
        }
        feeMapping = optionalFeeMapping.get();
        feeMapping.setUpdatedAt(new Date());
        feeMapping.setAmount(updateProductRequest.getAmount());
        return feeMappingRepository.save(feeMapping);
    }

    @Override
    public ResponseEntity<DefaultResponse> fetchAllProductWithFilterByAdmin(
        String categoryCode,
        String currencyCode,
        String country,
        String countryCode,
        ProductType productType,
        String provider,
        String code,
        int page,
        int pageSize
    ) {
        log.info("<<<<<Fetching all Product using filter by admin");
        ProductPage productPage = new ProductPage();
        productPage.setPageNo(page);
        productPage.setPageSize(pageSize);
        ProductSearchCriteria searchCriteria = getSearchCriteria(
            categoryCode,
            currencyCode,
            country,
            countryCode,
            productType,
            provider,
            code
        );
        Page<Product> products = queryService.getAllProductWithFilter(productPage, searchCriteria);
        DefaultResponse defaultResponse = new DefaultResponse();

        if (productPage.getPageSize() > 50) {
            defaultResponse.setMessage("Maximum page size exceeded");
            defaultResponse.setStatus(ResponseStatus.FAILED_REQUIREMENT.getCode());
        }

        Map<String, Object> map = new HashMap<>();
        map.put("totalPage", products.getTotalPages());
        map.put("totalContent", products.getTotalElements());
        map.put("items", products.getContent());
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setData(map);

        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DefaultResponse> fetchAllProductWithFilter(
        String categoryCode,
        String currencyCode,
        String country,
        String countryCode,
        ProductType productType,
        String provider,
        String code,
        int page,
        int pageSize
    ) {
        log.info(">>>>Fetching all Product using filter");
        ProductPage productPage = new ProductPage();
        productPage.setPageNo(page);
        productPage.setPageSize(pageSize);
        ProductSearchCriteria searchCriteria = getSearchCriteria(
            categoryCode,
            currencyCode,
            country,
            countryCode,
            productType,
            provider,
            code
        );
        Page<Product> products = queryService.getAllProductWithFilter(productPage, searchCriteria);
        DefaultResponse defaultResponse = new DefaultResponse();

        if (productPage.getPageSize() > 50) {
            defaultResponse.setMessage("Maximum page size exceeded");
            defaultResponse.setStatus(ResponseStatus.FAILED_REQUIREMENT.getCode());
        }

        Map<String, Object> map = new HashMap<>();
        map.put("totalPage", products.getTotalPages());
        map.put("totalContent", products.getTotalElements());
        List<DisplayProduct> displayProductList = products
            .getContent()
            .stream()
            .filter(p -> p.isActive())
            .map(product -> {
                DisplayProduct displayProduct = productMapper.mapToDisplayProduct(product);
                return displayProduct;
            })
            .collect(Collectors.toList());

        map.put("items", displayProductList);
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setData(map);

        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DefaultResponse> fetchProductByCode(String code) {
        log.info(">>> Fetching Product by code");
        Product product = productRepository
            .findByCode(code)
            .orElseThrow(() -> new NotFoundException(PRODUCT + ResponseStatus.NOT_FOUND.getMessage()));
        DefaultResponse defaultResponse = new DefaultResponse(
            ResponseStatus.SUCCESS.getMessage(),
            ResponseStatus.SUCCESS.getCode(),
            product
        );
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DefaultResponse> fetchProductByCategory(String categoryName, Pageable pageable) {
        log.info(">>> Fetching Product by category");
        Category category = categoryRepository
            .findByCode(categoryName)
            .orElseThrow(() -> new NotFoundException(CATEGORY + ResponseStatus.NOT_FOUND.getMessage()));

        Page<Product> product = productRepository.findByCategory(category, pageable);
        if (Objects.isNull(product)) {
            throw new NotFoundException(PRODUCT + ResponseStatus.NOT_FOUND.getMessage());
        }

        DefaultResponse defaultResponse = new DefaultResponse(
            ResponseStatus.SUCCESS.getMessage(),
            ResponseStatus.SUCCESS.getCode(),
            product
        );
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    private ProductSearchCriteria getSearchCriteria(
        String categoryCode,
        String currencyCode,
        String country,
        String countryCode,
        ProductType productType,
        String provider,
        String code
    ) {
        ProductSearchCriteria searchCriteria = new ProductSearchCriteria();
        searchCriteria.setCountryCode(countryCode);
        searchCriteria.setCountry(country);
        searchCriteria.setCurrencyCode(currencyCode);
        searchCriteria.setProductType(productType);
        searchCriteria.setCategoryCode(categoryCode);
        searchCriteria.setProvider(provider);
        searchCriteria.setCode(code);
        return searchCriteria;
    }

    @Override
    public ResponseEntity<DefaultResponse> processProductPrice(String productCode) {
        log.info(">>> processing Product Price with request: {}", productCode);
        Product product = productRepository
            .findByCode(productCode)
            .orElseThrow(() -> new NotFoundException(PRODUCT + ResponseStatus.NOT_FOUND.getMessage()));
        String processorId = vendingServiceProcessorService.getProcessorId(product.getCode());
        if (processorId == null) {
            log.info("Could not find processorId to use");
            throw new UnknownException(ResponseStatus.INTERNAL_SERVER_ERROR.getMessage(), ResponseStatus.INTERNAL_SERVER_ERROR.getCode());
        }

        log.info(">>> Getting the vendingService");
        AbstractVendingService vendingService = getVendingService(processorId);

        PriceResponse result = vendingService.getCurrentPrice(productCode);
        if (result == null) {
            throw new NotFoundException("Price details not found");
        }

        DefaultResponse defaultResponse = new DefaultResponse();
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setData(result);
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    @Override
    @Transactional
    public DefaultResponse bulkCreateProduct(List<CreateProductRequest> request) {
        try {
            List<Product> products = new ArrayList<>();
            for (CreateProductRequest req : request) {
                var product = preCreateProductAction(req);
                products.add(product);
            }

            log.info("Storing {} number of products", products.size());
            this.productRepository.saveAll(products);
            return DefaultResponse.builder().status("00").message("Products created successfully").build();
        } catch (AlreadyExistException | NotFoundException e) {
            log.info("Exception occurred whilst bulk creating products => {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.info("Exception occurred whilst bulk creating products => {}", e.getMessage());
            return DefaultResponse.builder().status("99").message("Failed to process request").build();
        }
    }

    private AbstractVendingService getVendingService(String processorId) {
        log.info(">>> Getting VendingService from pprocessorId):{}", processorId);
        AbstractVendingService serviceBean = vendingServiceDelegateBean.getDelegate(processorId);
        if (Objects.isNull(serviceBean)) {
            throw new UnknownException(INTERNAL_SERVER_ERROR.getMessage(), INTERNAL_SERVER_ERROR.getCode());
        }
        return serviceBean;
    }

    private Product preCreateProductAction(CreateProductRequest createProductRequest) {
        String code = createProductRequest.getCode().replaceAll("\\s+", "-");
        var optionalResultSet = productRepository.findProductExistsAndCategory(code, createProductRequest.getCategoryCode());

        if (optionalResultSet.isEmpty()) {
            throw new NotFoundException("Category with code provided not found");
        }

        Tuple resultSet = optionalResultSet.get();
        long productCount = resultSet.get(0, BigInteger.class).longValue();
        if (productCount > 0) {
            throw new AlreadyExistException(
                "The uploaded document contains one or more products with codes that already exist. The system identified '" +
                createProductRequest.getCode() +
                "' as one of the repeated codes. Please review and ensure all product codes are unique."
            );
        }

        Long catId = Objects.nonNull(resultSet.get(1)) ? resultSet.get(1, BigInteger.class).longValue() : null;
        if (Objects.isNull(catId)) {
            throw new NotFoundException(CATEGORY + ResponseStatus.NOT_FOUND.getMessage());
        }

        Category category = new Category();
        category.setId(catId);

        return productMapper.mapToEntity(createProductRequest, category);
    }
}
