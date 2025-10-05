package com.stan.stancore.extended.controller.admin;

import com.systemspecs.remita.annotation.ActivityTrail;
import com.systemspecs.remita.vending.extended.dto.request.CreateProviderRequest;
import com.systemspecs.remita.vending.extended.dto.request.UpdateProviderRequest;
import com.systemspecs.remita.vending.extended.dto.response.DefaultResponse;
import com.systemspecs.remita.vending.extended.service.ProviderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@SecurityRequirement(name = "Bearer Authentication")
@RequestMapping("/api/v1/admin/providers")
@RestController
@RequiredArgsConstructor
public class AdminProviderController {

    private final ProviderService providerService;

    @ActivityTrail
    @PostMapping
    public ResponseEntity<DefaultResponse> createProviders(@RequestBody @Valid CreateProviderRequest createProviderRequest) {
        return providerService.createProvider(createProviderRequest);
    }

    @ActivityTrail
    @PutMapping("/{code}")
    public ResponseEntity<DefaultResponse> updateProviders(@RequestBody UpdateProviderRequest request, @PathVariable String code) {
        return providerService.updateProvider(request, code);
    }

    @ActivityTrail
    @GetMapping
    public ResponseEntity<DefaultResponse> fetchAllProviders(
        HttpServletRequest request,
        @RequestParam(value = "code", required = false) String code,
        @RequestParam(value = "name", required = false) String name,
        @RequestParam(value = "description", required = false) String description,
        @RequestParam(required = false, defaultValue = "0", value = "page") int page,
        @RequestParam(required = false, defaultValue = "10", value = "pageSize") int pageSize
    ) {
        return providerService.fetchAllProviderWithFilterByAdmin(code, name, description, page, pageSize);
    }

    @ActivityTrail
    @GetMapping("/category/{category}")
    public ResponseEntity<DefaultResponse> fetchProductByCategory(
        @PathVariable("category") String category,
        @PageableDefault Pageable pageable
    ) {
        return providerService.fetchProviderByCategory(category, pageable);
    }
}
