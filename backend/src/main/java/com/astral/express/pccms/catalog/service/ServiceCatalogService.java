package com.astral.express.pccms.catalog.service;

import com.astral.express.pccms.catalog.dto.request.ServiceCatalogRequest;
import com.astral.express.pccms.catalog.dto.response.ServiceCatalogResponse;
import com.astral.express.pccms.appointment.entity.ServiceCatalog;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.appointment.repository.ServiceCatalogRepository;
import com.astral.express.pccms.catalog.service.ServiceCatalogService;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceCatalogService {

    private final ServiceCatalogRepository serviceCatalogRepository;
public PageResponse<ServiceCatalogResponse> searchServices(
            String keyword,
            ServiceCategory categoryCode,
            Boolean isActive,
            Pageable pageable) {
        Specification<ServiceCatalog> specification = combine(
                keywordContains(keyword),
                categoryEquals(categoryCode),
                activeEquals(isActive)
        );
        Page<ServiceCatalog> page = serviceCatalogRepository.findAll(specification, pageable);
        return PageResponse.of(page.map(this::toResponse));
    }
@Transactional
    public ServiceCatalogResponse createService(ServiceCatalogRequest request) {
        validateRequest(request);
        if (serviceCatalogRepository.existsByServiceCodeIgnoreCase(request.serviceCode())) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }

        ServiceCatalog service = new ServiceCatalog();
        applyRequest(service, request);
        return toResponse(serviceCatalogRepository.save(service));
    }
@Transactional
    public ServiceCatalogResponse updateService(UUID serviceId, ServiceCatalogRequest request) {
        validateRequest(request);
        ServiceCatalog service = findService(serviceId);
        if (serviceCatalogRepository.existsByServiceCodeIgnoreCaseAndIdNot(request.serviceCode(), serviceId)) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }

        applyRequest(service, request);
        return toResponse(serviceCatalogRepository.save(service));
    }
@Transactional
    public ServiceCatalogResponse deactivateService(UUID serviceId) {
        ServiceCatalog service = findService(serviceId);
        service.setIsActive(false);
        return toResponse(serviceCatalogRepository.save(service));
    }

    private ServiceCatalog findService(UUID serviceId) {
        return serviceCatalogRepository.findById(serviceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_404_NOT_FOUND));
    }

    private void validateRequest(ServiceCatalogRequest request) {
        if (request.basePriceVnd() == null || request.basePriceVnd() < 0) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        if (request.durationMinutes() != null && request.durationMinutes() <= 0) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        if (isInvalidEffectiveRange(request.effectiveFrom(), request.effectiveTo())) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }

    private boolean isInvalidEffectiveRange(LocalDate effectiveFrom, LocalDate effectiveTo) {
        return effectiveFrom != null && effectiveTo != null && effectiveTo.isBefore(effectiveFrom);
    }

    private void applyRequest(ServiceCatalog service, ServiceCatalogRequest request) {
        service.setServiceCode(request.serviceCode());
        service.setName(request.name());
        service.setCategoryCode(request.categoryCode());
        service.setDescription(request.description());
        service.setBasePriceVnd(request.basePriceVnd());
        service.setDurationMinutes(request.durationMinutes());
        service.setIsActive(request.isActive() == null || request.isActive());
        service.setEffectiveFrom(request.effectiveFrom());
        service.setEffectiveTo(request.effectiveTo());
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

    private Specification<ServiceCatalog> keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String normalizedKeyword = keyword.trim().toLowerCase();
        String pattern = "%" + normalizedKeyword + "%";
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("serviceCode")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern)
        );
    }

    private Specification<ServiceCatalog> categoryEquals(ServiceCategory categoryCode) {
        if (categoryCode == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("categoryCode"), categoryCode);
    }

    private Specification<ServiceCatalog> activeEquals(Boolean isActive) {
        if (isActive == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("isActive"), isActive);
    }

    @SafeVarargs
    private final Specification<ServiceCatalog> combine(Specification<ServiceCatalog>... specifications) {
        List<Specification<ServiceCatalog>> activeSpecifications = new ArrayList<>();
        for (Specification<ServiceCatalog> specification : specifications) {
            if (specification != null) {
                activeSpecifications.add(specification);
            }
        }
        if (activeSpecifications.isEmpty()) {
            return null;
        }
        Specification<ServiceCatalog> combined = activeSpecifications.getFirst();
        for (int index = 1; index < activeSpecifications.size(); index++) {
            combined = combined.and(activeSpecifications.get(index));
        }
        return combined;
    }
}



