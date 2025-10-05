package com.stan.stancore.extended.service.impl;

import com.systemspecs.remita.vending.extended.dto.request.CreateCategoryRequest;
import com.systemspecs.remita.vending.extended.dto.request.UpdateCategoryRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.enums.ResponseStatus;
import com.systemspecs.remita.vending.extended.exception.AlreadyExistException;
import com.systemspecs.remita.vending.extended.exception.NotFoundException;
import com.systemspecs.remita.vending.extended.service.CategoryService;
import com.systemspecs.remita.vending.vendingcommon.entity.Category;
import com.systemspecs.remita.vending.vendingcommon.repository.CategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class CategoryServiceImp implements CategoryService {

    private final CategoryRepository categoryRepository;
    private static final String CATEGORY = "category";

    public CategoryServiceImp(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public ResponseEntity<DefaultResponse> createCategory(CreateCategoryRequest request) {
        log.info(">>> Creating Category with request: {}", request);
        String code = request.getCode().replaceAll("\\s+", "-");
        Optional<Category> categoryCheck = categoryRepository.findByCode(code);
        if (categoryCheck.isPresent()) {
            throw new AlreadyExistException(CATEGORY + " " + ResponseStatus.ALREADY_EXIST.getMessage());
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setCode(code);
        category.setDescription(request.getDescription());
        category.setCreatedAt(new Date());

        categoryRepository.save(category);
        DefaultResponse defaultResponse = new DefaultResponse(
            ResponseStatus.SUCCESS.getMessage(),
            ResponseStatus.SUCCESS.getCode(),
            category
        );
        return new ResponseEntity<>(defaultResponse, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<DefaultResponse> updateCategory(UpdateCategoryRequest reuquest, String code) {
        log.info(">>> Updating Category with request: {} and code: {}", reuquest, code);
        Category categoryToUpdate = categoryRepository.findByCode(code).stream().findFirst().orElse(null);
        if (categoryToUpdate == null) {
            throw new NotFoundException(CATEGORY + ResponseStatus.NOT_FOUND.getMessage());
        }
        categoryToUpdate.setName(reuquest.getName());
        categoryToUpdate.setDescription(reuquest.getDescription());
        categoryToUpdate.setUpdatedAt(new Date());
        categoryRepository.save(categoryToUpdate);
        DefaultResponse defaultResponse = new DefaultResponse(
            ResponseStatus.SUCCESS.getMessage(),
            ResponseStatus.SUCCESS.getCode(),
            categoryToUpdate
        );
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DefaultResponse> fetchAllCategory(Pageable pageable) {
        DefaultResponse defaultResponse = new DefaultResponse();
        log.info("<<<<<Fetching all categories");
        Page<Category> categories = categoryRepository.findAll(pageable);

        Map<String, Object> map = new HashMap<>();
        map.put("totalPage", categories.getTotalPages());
        map.put("totalContent", categories.getTotalElements());
        map.put("items", categories.getContent());
        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        defaultResponse.setData(map);
        //        DefaultResponse defaultResponse = new DefaultResponse();
        //        defaultResponse.setStatus(ResponseStatus.SUCCESS.getCode());
        //        defaultResponse.setMessage(ResponseStatus.SUCCESS.getMessage());
        //        defaultResponse.setData(categories);
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DefaultResponse> fetchCategoryByCode(String code) {
        log.info(">>> Fetching Categories by code: {}", code);
        Category category = categoryRepository.findByCode(code).stream().findFirst().orElse(null);
        if (category == null) {
            throw new NotFoundException(CATEGORY + ResponseStatus.NOT_FOUND.getMessage());
        }
        DefaultResponse defaultResponse = new DefaultResponse(
            ResponseStatus.SUCCESS.getMessage(),
            ResponseStatus.SUCCESS.getCode(),
            category
        );
        return new ResponseEntity<>(defaultResponse, HttpStatus.OK);
    }
}
