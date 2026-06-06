package com.astral.express.pccms.catalog.service;

import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.catalog.dto.request.CreateServiceCatalogRequest;
import com.astral.express.pccms.catalog.dto.request.UpdateServiceCatalogRequest;
import com.astral.express.pccms.catalog.dto.response.ServiceCatalogResponse;
import com.astral.express.pccms.common.dto.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ServiceCatalogAdminService {
    ServiceCatalogResponse create(CreateServiceCatalogRequest request);
    ServiceCatalogResponse update(UUID id, UpdateServiceCatalogRequest request);
    ServiceCatalogResponse getById(UUID id);
    PageResponse<ServiceCatalogResponse> list(ServiceCategory categoryCode, Pageable pageable);
    void delete(UUID id);
    List<ServiceCategory> listCategories();
}
