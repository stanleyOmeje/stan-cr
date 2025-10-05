package com.stan.stancore.extended.controller.admin;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.vending.extended.dto.request.CreateProductRequest;
import com.systemspecs.remita.vending.extended.dto.request.UpdateProductRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.exception.AlreadyExistException;
import com.systemspecs.remita.vending.extended.exception.BadRequestException;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.ProductService;
import com.systemspecs.remita.vending.extended.util.CsvUtils;
import com.systemspecs.remita.vending.vendingcommon.enums.ProductType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.Valid;
import javax.validation.Validator;
import java.util.*;

@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/admin/products")
@RestController
@RequiredArgsConstructor
@Slf4j
public class AdminProductController {

    private final ProductService productService;
    private final Validator validator;

    @ActivityTrail
    @PostMapping
    public ResponseEntity<DefaultResponse> createProduct(@RequestBody @Valid CreateProductRequest createProductRequest) {
        return productService.createProduct(createProductRequest);
    }

    @ActivityTrail
    @PutMapping("/{code}")
    public ResponseEntity<DefaultResponse> updateProduct(@RequestBody @Valid UpdateProductRequest request, @PathVariable String code) {
        return productService.updateProduct(request, code);
    }

    @ActivityTrail
    @GetMapping("/price/{productCode}")
    public ResponseEntity<DefaultResponse> getProductPrice(@PathVariable String productCode) {
        return productService.processProductPrice(productCode);
    }

    @ActivityTrail
    @GetMapping
    public ResponseEntity<DefaultResponse> fetchAllProduct(
        HttpServletRequest request,
        @RequestParam(value = "countryCode", required = false) String countryCode,
        @RequestParam(value = "country", required = false) String country,
        @RequestParam(value = "productType", required = false) ProductType productType,
        @RequestParam(required = false, value = "categoryCode") String categoryCode,
        @RequestParam(required = false, value = "currencyCode") String currencyCode,
        @RequestParam(required = false, value = "provider") String provider,
        @RequestParam(required = false, value = "code") String code,
        @RequestParam(required = false, defaultValue = "0", value = "page") int page,
        @RequestParam(required = false, defaultValue = "10", value = "pageSize") int pageSize
    ) {
        return productService.fetchAllProductWithFilterByAdmin(
            categoryCode,
            currencyCode,
            country,
            countryCode,
            productType,
            provider,
            code,
            page,
            pageSize
        );
    }

    @ActivityTrail
    @PostMapping(path = "/bulk-create", consumes = "multipart/form-data")
    public ResponseEntity<DefaultResponse> bulkCreateProduct(@RequestParam("file") MultipartFile file) {
        try {
            List<CreateProductRequest> request = CsvUtils.read(CreateProductRequest.class, file.getInputStream());
            if (request.isEmpty()) {
                throw new BadRequestException("Product list cannot be empty", "99");
            }
            List<Map<String, Map<String, List<String>>>> error = new ArrayList<>();

            for (int i = 0; i < request.size(); i++) {
                var validationResult = validator.validate(request.get(i));

                if (!validationResult.isEmpty()) {
                    List<String> validationMessages = getValidationMessages(validationResult);

                    Map<String, Map<String, List<String>>> errorObj = new HashMap<>();

                    errorObj.put(String.format("Product %s", i + 1), Map.of("errors", validationMessages));
                    error.add(errorObj);
                }
            }

            if (!error.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    DefaultResponse
                        .builder()
                        .status("99")
                        .message("Missing required fields")
                        .data(error)
                        .build()
                );
            }
            return ResponseEntity.ok(productService.bulkCreateProduct(request));
        } catch (AlreadyExistException | NotFoundException e) {
            throw e;
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(DefaultResponse.builder().status("99").message("Failed to process request").build());
        }
    }

    private static List<String> getValidationMessages(Set<ConstraintViolation<CreateProductRequest>> validationResult) {
        List<String> validationMessages = new ArrayList<>();
        var resultArray = new ArrayList<>(validationResult);
        for (ConstraintViolation<CreateProductRequest> createProductRequestConstraintViolation : resultArray) {
            String message = String.format("%s %s", createProductRequestConstraintViolation.getPropertyPath(), createProductRequestConstraintViolation.getMessage());
            validationMessages.add(message);
        }
        return validationMessages;
    }
}
