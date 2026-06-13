package com.astral.express.pccms.catalog.service;

import com.astral.express.pccms.appointment.entity.ServiceCatalog;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.appointment.repository.ServiceCatalogRepository;
import com.astral.express.pccms.appointment.repository.ServiceOrderRepository;
import com.astral.express.pccms.catalog.dto.request.CreateServiceCatalogRequest;
import com.astral.express.pccms.catalog.dto.request.UpdateServiceCatalogRequest;
import com.astral.express.pccms.catalog.dto.response.ServiceCatalogResponse;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServiceCatalogAdminServiceTest {

    @Mock
    private ServiceCatalogRepository serviceCatalogRepository;
    
    @Mock
    private ServiceOrderRepository serviceOrderRepository;

    @InjectMocks
    private ServiceCatalogAdminService serviceCatalogAdminService;

    @Test
    void should_CreateServiceCatalog_Success() {
        // GIVEN
        CreateServiceCatalogRequest request = new CreateServiceCatalogRequest(
                "CODE", "Name", ServiceCategory.GROOMING, "Desc", 100L, 30, true
        );
        given(serviceCatalogRepository.existsByServiceCode("CODE")).willReturn(false);
        given(serviceCatalogRepository.existsByName("Name")).willReturn(false);

        ServiceCatalog savedService = new ServiceCatalog();
        savedService.setId(UUID.randomUUID());
        savedService.setServiceCode("CODE");
        given(serviceCatalogRepository.save(any(ServiceCatalog.class))).willReturn(savedService);

        // WHEN
        ServiceCatalogResponse response = serviceCatalogAdminService.create(request);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.serviceCode()).isEqualTo("CODE");
    }

    @Test
    void should_ThrowException_when_CreateWithExistingCode() {
        // GIVEN
        CreateServiceCatalogRequest request = new CreateServiceCatalogRequest(
                "CODE", "Name", ServiceCategory.GROOMING, "Desc", 100L, 30, true
        );
        given(serviceCatalogRepository.existsByServiceCode("CODE")).willReturn(true);

        // WHEN & THEN
        assertThatThrownBy(() -> serviceCatalogAdminService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_SVC_002_CODE_EXISTS);
    }

    @Test
    void should_UpdateServiceCatalog_Success() {
        // GIVEN
        UUID id = UUID.randomUUID();
        UpdateServiceCatalogRequest request = new UpdateServiceCatalogRequest(
                "CODE", "Name", ServiceCategory.GROOMING, "Desc", 100L, 30, true
        );
        ServiceCatalog existingService = new ServiceCatalog();
        existingService.setId(id);
        
        given(serviceCatalogRepository.findById(id)).willReturn(Optional.of(existingService));
        given(serviceCatalogRepository.existsByServiceCodeAndIdNot("CODE", id)).willReturn(false);
        given(serviceCatalogRepository.existsByNameAndIdNot("Name", id)).willReturn(false);
        given(serviceCatalogRepository.save(any(ServiceCatalog.class))).willReturn(existingService);

        // WHEN
        ServiceCatalogResponse response = serviceCatalogAdminService.update(id, request);

        // THEN
        assertThat(response).isNotNull();
        verify(serviceCatalogRepository).save(any(ServiceCatalog.class));
    }

    @Test
    void should_GetById_Success() {
        // GIVEN
        UUID id = UUID.randomUUID();
        ServiceCatalog existingService = new ServiceCatalog();
        existingService.setId(id);
        given(serviceCatalogRepository.findById(id)).willReturn(Optional.of(existingService));

        // WHEN
        ServiceCatalogResponse response = serviceCatalogAdminService.getById(id);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
    }

    @Test
    void should_List_Success() {
        // GIVEN
        ServiceCatalog service = new ServiceCatalog();
        service.setId(UUID.randomUUID());
        given(serviceCatalogRepository.findAll(any(PageRequest.class)))
                .willReturn(new PageImpl<>(List.of(service)));

        // WHEN
        PageResponse<ServiceCatalogResponse> response = serviceCatalogAdminService.list(null, PageRequest.of(0, 10));

        // THEN
        assertThat(response.data().content()).hasSize(1);
    }

    @Test
    void should_Delete_Success() {
        // GIVEN
        UUID id = UUID.randomUUID();
        ServiceCatalog service = new ServiceCatalog();
        service.setId(id);
        service.setIsActive(true);
        
        given(serviceCatalogRepository.findById(id)).willReturn(Optional.of(service));
        given(serviceOrderRepository.existsByService_Id(id)).willReturn(false);

        // WHEN
        serviceCatalogAdminService.delete(id);

        // THEN
        verify(serviceCatalogRepository).save(service);
        assertThat(service.getIsActive()).isFalse();
    }

    @Test
    void should_ThrowException_when_DeleteServiceInUse() {
        // GIVEN
        UUID id = UUID.randomUUID();
        ServiceCatalog service = new ServiceCatalog();
        service.setId(id);
        
        given(serviceCatalogRepository.findById(id)).willReturn(Optional.of(service));
        given(serviceOrderRepository.existsByService_Id(id)).willReturn(true);

        // WHEN & THEN
        assertThatThrownBy(() -> serviceCatalogAdminService.delete(id))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_SVC_004_IN_USE);
    }

    @org.junit.jupiter.api.Test
    void update_shouldThrowException_whenNotFound() {
        UUID id = UUID.randomUUID();
        UpdateServiceCatalogRequest request = new UpdateServiceCatalogRequest("SVC1", "Service", ServiceCategory.MEDICAL, "", 100L, 30, true);
        given(serviceCatalogRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> serviceCatalogAdminService.update(id, request))
                .isInstanceOf(BusinessException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_SVC_001_NOT_FOUND);
    }

    @org.junit.jupiter.api.Test
    void update_shouldThrowException_whenNameExistsForOtherService() {
        UUID id = UUID.randomUUID();
        UpdateServiceCatalogRequest request = new UpdateServiceCatalogRequest("SVC1", "Service", ServiceCategory.MEDICAL, "", 100L, 30, true);
        ServiceCatalog service = new ServiceCatalog();
        given(serviceCatalogRepository.findById(id)).willReturn(Optional.of(service));
        given(serviceCatalogRepository.existsByServiceCodeAndIdNot("SVC1", id)).willReturn(false);
        given(serviceCatalogRepository.existsByNameAndIdNot("Service", id)).willReturn(true);

        assertThatThrownBy(() -> serviceCatalogAdminService.update(id, request))
                .isInstanceOf(BusinessException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_SVC_003_NAME_EXISTS);
    }

    @org.junit.jupiter.api.Test
    void getById_shouldThrowException_whenNotFound() {
        UUID id = UUID.randomUUID();
        given(serviceCatalogRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> serviceCatalogAdminService.getById(id))
                .isInstanceOf(BusinessException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_SVC_001_NOT_FOUND);
    }

    @org.junit.jupiter.api.Test
    void delete_shouldThrowException_whenNotFound() {
        UUID id = UUID.randomUUID();
        given(serviceCatalogRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> serviceCatalogAdminService.delete(id))
                .isInstanceOf(BusinessException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_SVC_001_NOT_FOUND);
    }

    @org.junit.jupiter.api.Test
    void list_shouldReturnPage_withCategoryFilter() {
        PageRequest pageable = PageRequest.of(0, 10);
        ServiceCatalog service = new ServiceCatalog();
        service.setId(UUID.randomUUID());
        given(serviceCatalogRepository.findByCategoryCode(ServiceCategory.MEDICAL, pageable))
                .willReturn(new PageImpl<>(List.of(service), pageable, 1));

        var response = serviceCatalogAdminService.list(ServiceCategory.MEDICAL, pageable);
        assertThat(response.data().content()).hasSize(1);
    }

    @org.junit.jupiter.api.Test
    void listCategories_shouldReturnAllCategories() {
        List<ServiceCategory> categories = serviceCatalogAdminService.listCategories();
        assertThat(categories).containsExactly(ServiceCategory.values());
    }

}
