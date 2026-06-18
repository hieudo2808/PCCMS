package com.astral.express.pccms.grooming.service;

import com.astral.express.pccms.appointment.entity.ServiceCatalog;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.appointment.repository.ServiceCatalogRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.grooming.dto.request.GroomingServiceRequest;
import com.astral.express.pccms.grooming.dto.response.GroomingServiceResponse;
import com.astral.express.pccms.grooming.mapper.GroomingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroomingCatalogAdminService {
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final GroomingMapper groomingMapper;

    @Transactional(readOnly = true)
    public List<GroomingServiceResponse> listGroomingServicesForAdmin() {
        return serviceCatalogRepository.findByCategoryCodeAndIsActiveTrueOrderByNameAsc(ServiceCategory.GROOMING).stream()
                .map(groomingMapper::toServiceResponse)
                .toList();
    }

    @Transactional
    public GroomingServiceResponse createGroomingService(GroomingServiceRequest request) {
        validateGroomingServiceRequest(request);
        if (serviceCatalogRepository.existsByServiceCode(request.serviceCode())) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_007_SERVICE_CODE_EXISTS);
        }
        ServiceCatalog service = new ServiceCatalog();
        service.setServiceCode(request.serviceCode());
        service.setName(request.name());
        service.setCategoryCode(ServiceCategory.GROOMING);
        service.setDescription(request.description());
        service.setBasePriceVnd(request.basePriceVnd());
        service.setDurationMinutes(request.durationMinutes());
        service.setIsActive(true);
        return groomingMapper.toServiceResponse(serviceCatalogRepository.save(service));
    }

    @Transactional
    public GroomingServiceResponse updateGroomingService(UUID id, GroomingServiceRequest request) {
        validateGroomingServiceRequest(request);
        ServiceCatalog service = findGroomingService(id);
        if (serviceCatalogRepository.existsByServiceCodeAndIdNot(request.serviceCode(), id)) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_007_SERVICE_CODE_EXISTS);
        }
        service.setServiceCode(request.serviceCode());
        service.setName(request.name());
        service.setDescription(request.description());
        service.setBasePriceVnd(request.basePriceVnd());
        service.setDurationMinutes(request.durationMinutes());
        return groomingMapper.toServiceResponse(serviceCatalogRepository.save(service));
    }

    @Transactional
    public void deactivateGroomingService(UUID id) {
        ServiceCatalog service = findGroomingService(id);
        service.setIsActive(false);
        serviceCatalogRepository.save(service);
    }

    private ServiceCatalog findGroomingService(UUID id) {
        return serviceCatalogRepository.findById(id)
                .filter(c -> c.getCategoryCode() == ServiceCategory.GROOMING)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_GROOMING_002_SERVICE_NOT_FOUND));
    }

    private void validateGroomingServiceRequest(GroomingServiceRequest request) {
        if (request.basePriceVnd() == null || request.basePriceVnd() < 0) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        if (request.durationMinutes() == null || request.durationMinutes() < 1) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }
}
