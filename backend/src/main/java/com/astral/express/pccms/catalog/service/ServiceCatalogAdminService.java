package com.astral.express.pccms.catalog.service;

import com.astral.express.pccms.appointment.entity.ServiceCatalog;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.appointment.repository.ServiceCatalogRepository;
import com.astral.express.pccms.appointment.repository.ServiceOrderRepository;
import com.astral.express.pccms.catalog.dto.request.CreateServiceCatalogRequest;
import com.astral.express.pccms.catalog.dto.request.UpdateServiceCatalogRequest;
import com.astral.express.pccms.catalog.dto.response.ServiceCatalogResponse;
import com.astral.express.pccms.catalog.service.ServiceCatalogAdminService;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceCatalogAdminService {

    private final ServiceCatalogRepository serviceCatalogRepository;
    private final ServiceOrderRepository serviceOrderRepository;
@Transactional
    public ServiceCatalogResponse create(CreateServiceCatalogRequest request) {
        ensureUniqueCode(request.serviceCode(), null);
        ensureUniqueName(request.name(), null);

        ServiceCatalog service = new ServiceCatalog();
        applyCreateRequest(service, request);
        return toResponse(serviceCatalogRepository.save(service));
    }
@Transactional
    public ServiceCatalogResponse update(UUID id, UpdateServiceCatalogRequest request) {
        ServiceCatalog service = serviceCatalogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_SVC_001_NOT_FOUND));

        ensureUniqueCode(request.serviceCode(), id);
        ensureUniqueName(request.name(), id);

        applyUpdateRequest(service, request);
        return toResponse(serviceCatalogRepository.save(service));
    }
@Transactional(readOnly = true)
    public ServiceCatalogResponse getById(UUID id) {
        return serviceCatalogRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_SVC_001_NOT_FOUND));
    }
@Transactional(readOnly = true)
    public PageResponse<ServiceCatalogResponse> list(ServiceCategory categoryCode, Pageable pageable) {
        Page<ServiceCatalog> page = categoryCode != null
                ? serviceCatalogRepository.findByCategoryCode(categoryCode, pageable)
                : serviceCatalogRepository.findAll(pageable);
        return PageResponse.of(page.map(this::toResponse));
    }
@Transactional
    public void delete(UUID id) {
        ServiceCatalog service = serviceCatalogRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_SVC_001_NOT_FOUND));
        if (serviceOrderRepository.existsByService_Id(id)) {
            throw new BusinessException(ErrorCode.ERR_SVC_004_IN_USE);
        }
        service.setIsActive(false);
        serviceCatalogRepository.save(service);
    }
@Transactional(readOnly = true)
    public List<ServiceCategory> listCategories() {
        return Arrays.asList(ServiceCategory.values());
    }

    private void applyCreateRequest(ServiceCatalog service, CreateServiceCatalogRequest request) {
        service.setServiceCode(request.serviceCode());
        service.setName(request.name());
        service.setCategoryCode(request.categoryCode());
        service.setDescription(request.description());
        service.setBasePriceVnd(request.basePriceVnd());
        service.setDurationMinutes(request.durationMinutes());
        service.setIsActive(request.isActive());
    }

    private void applyUpdateRequest(ServiceCatalog service, UpdateServiceCatalogRequest request) {
        service.setServiceCode(request.serviceCode());
        service.setName(request.name());
        service.setCategoryCode(request.categoryCode());
        service.setDescription(request.description());
        service.setBasePriceVnd(request.basePriceVnd());
        service.setDurationMinutes(request.durationMinutes());
        service.setIsActive(request.isActive());
    }

    private void ensureUniqueCode(String serviceCode, UUID excludeId) {
        boolean exists = excludeId == null
                ? serviceCatalogRepository.existsByServiceCode(serviceCode)
                : serviceCatalogRepository.existsByServiceCodeAndIdNot(serviceCode, excludeId);
        if (exists) {
            throw new BusinessException(ErrorCode.ERR_SVC_002_CODE_EXISTS);
        }
    }

    private void ensureUniqueName(String name, UUID excludeId) {
        boolean exists = excludeId == null
                ? serviceCatalogRepository.existsByName(name)
                : serviceCatalogRepository.existsByNameAndIdNot(name, excludeId);
        if (exists) {
            throw new BusinessException(ErrorCode.ERR_SVC_003_NAME_EXISTS);
        }
    }

    private ServiceCatalogResponse toResponse(ServiceCatalog service) {
        return new ServiceCatalogResponse(
                service.getId(),
                service.getServiceCode(),
                service.getName(),
                service.getCategoryCode(),
                service.getDescription(),
                service.getBasePriceVnd(),
                service.getDurationMinutes(),
                service.getIsActive(),
                service.getEffectiveFrom(),
                service.getEffectiveTo(),
                service.getCreatedAt(),
                service.getUpdatedAt()
        );
    }
}


