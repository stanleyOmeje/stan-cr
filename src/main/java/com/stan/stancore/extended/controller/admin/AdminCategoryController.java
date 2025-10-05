package com.stan.stancore.extended.controller.admin;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.vending.extended.dto.request.CreateCategoryRequest;
import com.systemspecs.remita.vending.extended.dto.request.UpdateCategoryRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.service.CategoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/admin/categories")
@RestController
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @ActivityTrail
    @PostMapping
    public ResponseEntity<DefaultResponse> createCategories(@RequestBody @Valid CreateCategoryRequest createCategoryRequest) {
        return categoryService.createCategory(createCategoryRequest);
    }

    @ActivityTrail
    @PutMapping("/{code}")
    public ResponseEntity<DefaultResponse> updateCategories(@RequestBody UpdateCategoryRequest request, @PathVariable String code) {
        return categoryService.updateCategory(request, code);
    }

    @ActivityTrail
    @GetMapping
    public ResponseEntity<DefaultResponse> fetchCategories(HttpServletRequest request, Pageable pageable) {
        return categoryService.fetchAllCategory(pageable);
    }
}
