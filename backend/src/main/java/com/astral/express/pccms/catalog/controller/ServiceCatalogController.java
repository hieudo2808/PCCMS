package com.astral.express.pccms.catalog.controller;

import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.catalog.dto.request.CreateServiceCatalogRequest;
import com.astral.express.pccms.catalog.dto.request.UpdateServiceCatalogRequest;
import com.astral.express.pccms.catalog.dto.response.ServiceCatalogResponse;
import com.astral.express.pccms.catalog.security.CatalogPermissions;
import com.astral.express.pccms.catalog.service.ServiceCatalogAdminService;
import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/catalog/services")
@RequiredArgsConstructor
public class ServiceCatalogController {

    private final ServiceCatalogAdminService serviceCatalogAdminService;

    @PreAuthorize(CatalogPermissions.SERVICE_MANAGE)
    @PostMapping
    public ApiResponse<ServiceCatalogResponse> create(@Valid @RequestBody CreateServiceCatalogRequest request) {
        return ApiResponse.created(serviceCatalogAdminService.create(request));
    }

    @PreAuthorize(CatalogPermissions.SERVICE_MANAGE)
    @PutMapping("/{id}")
    public ApiResponse<ServiceCatalogResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServiceCatalogRequest request) {
        return ApiResponse.success(serviceCatalogAdminService.update(id, request));
    }

    @PreAuthorize(CatalogPermissions.SERVICE_READ)
    @GetMapping("/categories")
    public ApiResponse<List<ServiceCategory>> listCategories() {
        return ApiResponse.success(serviceCatalogAdminService.listCategories());
    }

    @PreAuthorize(CatalogPermissions.SERVICE_READ)
    @GetMapping
    public ApiResponse<PageResponse<ServiceCatalogResponse>> list(
            @RequestParam(required = false) ServiceCategory categoryCode,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(serviceCatalogAdminService.list(categoryCode, pageable));
    }

    @PreAuthorize(CatalogPermissions.SERVICE_READ)
    @GetMapping("/{id}")
    public ApiResponse<ServiceCatalogResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(serviceCatalogAdminService.getById(id));
    }

    @PreAuthorize(CatalogPermissions.SERVICE_MANAGE)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        serviceCatalogAdminService.delete(id);
        return ApiResponse.success(null);
    }
}
