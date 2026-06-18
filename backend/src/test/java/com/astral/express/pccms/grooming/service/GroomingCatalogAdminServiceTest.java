package com.astral.express.pccms.grooming.service;

import com.astral.express.pccms.appointment.entity.ServiceCatalog;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.appointment.repository.ServiceCatalogRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.grooming.dto.request.GroomingServiceRequest;
import com.astral.express.pccms.grooming.mapper.GroomingMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GroomingCatalogAdminServiceTest {

    @Mock
    private ServiceCatalogRepository serviceCatalogRepository;

    private GroomingCatalogAdminService service;

    @BeforeEach
    void setUp() {
        service = new GroomingCatalogAdminService(serviceCatalogRepository, new GroomingMapper());
    }

    @Test
    void createGroomingService_setsGroomingCategoryAndActiveStatus() {
        GroomingServiceRequest request = new GroomingServiceRequest(
                "GRM-BATH",
                "Tam say",
                "Cham soc long co ban",
                120000L,
                45
        );
        given(serviceCatalogRepository.existsByServiceCode("GRM-BATH")).willReturn(false);
        given(serviceCatalogRepository.save(any(ServiceCatalog.class))).willAnswer(invocation -> {
            ServiceCatalog catalog = invocation.getArgument(0);
            catalog.setId(UUID.randomUUID());
            return catalog;
        });

        var response = service.createGroomingService(request);

        ArgumentCaptor<ServiceCatalog> captor = ArgumentCaptor.forClass(ServiceCatalog.class);
        verify(serviceCatalogRepository).save(captor.capture());
        ServiceCatalog saved = captor.getValue();
        assertThat(saved.getCategoryCode()).isEqualTo(ServiceCategory.GROOMING);
        assertThat(saved.getIsActive()).isTrue();
        assertThat(response.serviceCode()).isEqualTo("GRM-BATH");
    }

    @Test
    void createGroomingService_rejectsDuplicateCode() {
        GroomingServiceRequest request = new GroomingServiceRequest("GRM-BATH", "Tam say", null, 120000L, 45);
        given(serviceCatalogRepository.existsByServiceCode("GRM-BATH")).willReturn(true);

        assertThatThrownBy(() -> service.createGroomingService(request))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ERR_GROOMING_007_SERVICE_CODE_EXISTS));
    }

    @Test
    void updateGroomingService_rejectsNonGroomingCatalog() {
        UUID id = UUID.randomUUID();
        ServiceCatalog medicalService = ServiceCatalog.builder()
                .id(id)
                .serviceCode("MED-EXAM")
                .name("Kham benh")
                .categoryCode(ServiceCategory.MEDICAL)
                .basePriceVnd(100000L)
                .durationMinutes(30)
                .isActive(true)
                .build();
        given(serviceCatalogRepository.findById(id)).willReturn(Optional.of(medicalService));
        GroomingServiceRequest request = new GroomingServiceRequest("GRM-BATH", "Tam say", null, 120000L, 45);

        assertThatThrownBy(() -> service.updateGroomingService(id, request))
                .isInstanceOfSatisfying(BusinessException.class, ex ->
                        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ERR_GROOMING_002_SERVICE_NOT_FOUND));
    }
}
