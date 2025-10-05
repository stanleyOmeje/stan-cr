package com.stan.stancore.extended.dto.request.ServiceMapper;

import com.systemspecs.remita.vending.extended.dto.request.CreateProductRequest;
import com.systemspecs.remita.vending.extended.dto.request.DisplayProduct;
import com.systemspecs.remita.vending.vendingcommon.entity.Category;
import com.systemspecs.remita.vending.vendingcommon.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class ProductMapper {

    public Product mapToEntity(CreateProductRequest createProductRequest, Category category) {
        log.info(">>> Mapping createProductRequest to product");
        Product product = new Product();
        product.setName(createProductRequest.getName());
        product.setDescription(createProductRequest.getDescription());
        product.setCode(createProductRequest.getCode().replaceAll("\\s+", "-"));
        product.setCreatedAt(new Date());
        product.setCategory(category);
        product.setCountry(createProductRequest.getCountry());
        product.setCountryCode(createProductRequest.getCountryCode());
        product.setCurrencyCode(createProductRequest.getCurrencyCode());
        product.setProductType(createProductRequest.getProductType());
        product.setCalculationMode(createProductRequest.getCalculationMode());
        product.setApplyCommission(createProductRequest.getApplyCommission());
        product.setIsFixedCommission(createProductRequest.getIsFixedCommission());
        product.setFixedCommission(createProductRequest.getFixedCommission());
        product.setPercentageCommission(createProductRequest.getPercentageCommission());
        product.setPercentageMinCap(createProductRequest.getPercentageMinCap());
        product.setPercentageMaxCap(createProductRequest.getPercentageMaxCap());
        product.setProvider(createProductRequest.getProvider());
        product.setActive(createProductRequest.getActive());
        return product;
    }

    public DisplayProduct mapToDisplayProduct(Product product) {
        log.info(">>> Mapping Product to DisplayProduct");
        DisplayProduct displayProductRequest = new DisplayProduct();
        displayProductRequest.setName(product.getName());
        displayProductRequest.setDescription(product.getDescription());
        displayProductRequest.setCode(product.getCode().replaceAll("\\s+", "-"));
        displayProductRequest.setCreatedAt(new Date());
        displayProductRequest.setCategory(product.getCategory());
        displayProductRequest.setCountry(product.getCountry());
        displayProductRequest.setCountryCode(product.getCountryCode());
        displayProductRequest.setCurrencyCode(product.getCurrencyCode());
        displayProductRequest.setProductType(product.getProductType());
        if(product.getFee() != null) {
            displayProductRequest.setFeeType(product.getFee().getFeeType());
            displayProductRequest.setAmount(product.getFee().getAmount());
        }
        displayProductRequest.setProvider(product.getProvider());

        return displayProductRequest;
    }
}
