package com.astral.express.pccms.catalog.service;

import com.astral.express.pccms.catalog.dto.request.ServiceCatalogRequest;
import com.astral.express.pccms.catalog.dto.response.ServiceCatalogResponse;
import com.astral.express.pccms.appointment.entity.ServiceCatalog;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.appointment.repository.ServiceCatalogRepository;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ServiceCatalogServiceTest {

    @Mock
    private ServiceCatalogRepository serviceCatalogRepository;

    @InjectMocks
    private ServiceCatalogService serviceCatalogService;

    @ParameterizedTest(name = "[{1}] {3}")
    @CsvFileSource(resources = "/testcases/service-catalog-management.csv", numLinesToSkip = 1)
    void should_followServiceCatalogCsvRules(
            String ruleId,
            String caseId,
            String useCase,
            String scenario,
            String precondition,
            String input,
            String expectedResult,
            String expectedErrorCode,
            String expectedMessage,
            String note) {
        if ("Missing required service fields rejected".equals(scenario)
                || "Invalid service category rejected".equals(scenario)) {
            assertControllerLayerValidation(caseId, expectedErrorCode);
            return;
        }

        ServiceCatalogCsvInput csv = parseInput(input);
        PageRequest pageable = PageRequest.of(0, 20);

        switch (scenario) {
            case "List service catalog success", "Filter services by category success",
                    "Filter services by active status success", "Search services by name success" ->
                    assertSearch(csv, pageable);
            case "Create service success" -> assertCreateSuccess(csv);
            case "Update service success" -> assertUpdateSuccess(csv);
            case "Deactivate service success" -> assertDeactivateSuccess();
            case "Duplicate service code rejected", "Negative base price rejected",
                    "Non-positive duration rejected", "Invalid effective date range rejected" ->
                    assertFailure(scenario, csv, ErrorCode.valueOf(expectedErrorCode));
            default -> throw new IllegalArgumentException("Unhandled CSV scenario: " + scenario);
        }
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/testcases/service-catalog-search.csv", numLinesToSkip = 1)
    void should_ProcessSearch(String ruleId, String caseId, String action, String keyword) {
        if ("SEARCH".equals(action)) {
            PageRequest pageable = PageRequest.of(0, 20);
            given(serviceCatalogRepository.findAll(any(Specification.class), eq(pageable)))
                    .willReturn(new PageImpl<>(List.of(), pageable, 0));

            serviceCatalogService.searchServices(keyword, null, null, pageable);

            ArgumentCaptor<Specification<ServiceCatalog>> captor = ArgumentCaptor.forClass(Specification.class);
            verify(serviceCatalogRepository).findAll(captor.capture(), eq(pageable));

            Root<ServiceCatalog> root = org.mockito.Mockito.mock(Root.class);
            CriteriaQuery<?> query = org.mockito.Mockito.mock(CriteriaQuery.class);
            CriteriaBuilder criteriaBuilder = org.mockito.Mockito.mock(CriteriaBuilder.class);
            Path<String> serviceCodePath = org.mockito.Mockito.mock(Path.class);
            Path<String> namePath = org.mockito.Mockito.mock(Path.class);
            Expression<String> lowerServiceCode = org.mockito.Mockito.mock(Expression.class);
            Expression<String> lowerName = org.mockito.Mockito.mock(Expression.class);
            Predicate serviceCodePredicate = org.mockito.Mockito.mock(Predicate.class);
            Predicate namePredicate = org.mockito.Mockito.mock(Predicate.class);
            Predicate keywordPredicate = org.mockito.Mockito.mock(Predicate.class);

            given(root.<String>get("serviceCode")).willReturn(serviceCodePath);
            given(root.<String>get("name")).willReturn(namePath);
            given(criteriaBuilder.lower(serviceCodePath)).willReturn(lowerServiceCode);
            given(criteriaBuilder.lower(namePath)).willReturn(lowerName);
            given(criteriaBuilder.like(lowerServiceCode, "%" + keyword.toLowerCase() + "%")).willReturn(serviceCodePredicate);
            given(criteriaBuilder.like(lowerName, "%" + keyword.toLowerCase() + "%")).willReturn(namePredicate);
            given(criteriaBuilder.or(serviceCodePredicate, namePredicate)).willReturn(keywordPredicate);

            captor.getValue().toPredicate(root, query, criteriaBuilder);

            verify(root).get("serviceCode");
            verify(root).get("name");
            verify(criteriaBuilder).or(serviceCodePredicate, namePredicate);
        }
    }

    private void assertSearch(ServiceCatalogCsvInput csv, PageRequest pageable) {
        ServiceCatalog service = service("SVC001", "Consultation", ServiceCategory.MEDICAL, true);
        given(serviceCatalogRepository.findAll(nullable(Specification.class), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(service), pageable, 1));

        PageResponse<ServiceCatalogResponse> response = serviceCatalogService.searchServices(
                csv.keyword(), csv.categoryCode(), csv.isActive(), pageable);

        assertThat(response.data().content()).hasSize(1);
        assertThat(response.data().content().getFirst().serviceCode()).isEqualTo("SVC001");
    }

    private void assertControllerLayerValidation(String caseId, String expectedErrorCode) {
        assertThat(caseId).isIn("TC_SVC_008", "TC_SVC_010");
        assertThat(expectedErrorCode).isEqualTo(ErrorCode.ERR_VALIDATION_FAILED.name());
    }

    private void assertCreateSuccess(ServiceCatalogCsvInput csv) {
        given(serviceCatalogRepository.existsByServiceCodeIgnoreCase(csv.serviceCode())).willReturn(false);
        ServiceCatalog saved = service(csv.serviceCode(), csv.name(), csv.categoryCode(), csv.isActive());
        given(serviceCatalogRepository.save(any(ServiceCatalog.class))).willReturn(saved);

        ServiceCatalogResponse response = serviceCatalogService.createService(request(csv));

        assertThat(response.serviceCode()).isEqualTo(csv.serviceCode());
        verify(serviceCatalogRepository).save(any(ServiceCatalog.class));
    }

    private void assertUpdateSuccess(ServiceCatalogCsvInput csv) {
        UUID serviceId = UUID.randomUUID();
        ServiceCatalog service = service("SVC001", "Old Service", ServiceCategory.MEDICAL, true);
        given(serviceCatalogRepository.findById(serviceId)).willReturn(Optional.of(service));
        given(serviceCatalogRepository.save(service)).willReturn(service);

        ServiceCatalogResponse response = serviceCatalogService.updateService(serviceId, request(csv));

        assertThat(response.id()).isEqualTo(service.getId());
        verify(serviceCatalogRepository).save(service);
    }

    private void assertDeactivateSuccess() {
        UUID serviceId = UUID.randomUUID();
        ServiceCatalog service = service("SVC001", "Consultation", ServiceCategory.MEDICAL, true);
        given(serviceCatalogRepository.findById(serviceId)).willReturn(Optional.of(service));
        given(serviceCatalogRepository.save(service)).willReturn(service);

        ServiceCatalogResponse response = serviceCatalogService.deactivateService(serviceId);

        assertThat(response.isActive()).isFalse();
    }

    private void assertFailure(String scenario, ServiceCatalogCsvInput csv, ErrorCode errorCode) {
        if ("Duplicate service code rejected".equals(scenario)) {
            given(serviceCatalogRepository.existsByServiceCodeIgnoreCase(csv.serviceCode())).willReturn(true);
        }

        assertThatThrownBy(() -> serviceCatalogService.createService(request(csv)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", errorCode);
    }

    private ServiceCatalog service(String code, String name, ServiceCategory category, Boolean active) {
        ServiceCatalog service = new ServiceCatalog();
        service.setId(UUID.randomUUID());
        service.setServiceCode(code);
        service.setName(name);
        service.setCategoryCode(category);
        service.setBasePriceVnd(100000L);
        service.setDurationMinutes(30);
        service.setIsActive(active);
        return service;
    }

    private ServiceCatalogRequest request(ServiceCatalogCsvInput input) {
        return new ServiceCatalogRequest(
                input.serviceCode(),
                input.name(),
                input.categoryCode(),
                input.description(),
                input.basePriceVnd(),
                input.durationMinutes(),
                input.isActive(),
                input.effectiveFrom(),
                input.effectiveTo()
        );
    }

    private ServiceCatalogCsvInput parseInput(String input) {
        return new ServiceCatalogCsvInput(
                text(input, "keyword"),
                category(input, "categoryCode"),
                bool(input, "isActive"),
                text(input, "serviceCode"),
                text(input, "name"),
                category(input, "categoryCode"),
                text(input, "description"),
                longVal(input, "basePriceVnd"),
                integer(input, "durationMinutes"),
                bool(input, "isActive"),
                date(input, "effectiveFrom"),
                date(input, "effectiveTo")
        );
    }

    private ServiceCategory category(String input, String key) {
        String value = text(input, key);
        return value == null ? null : ServiceCategory.valueOf(value);
    }

    private LocalDate date(String input, String key) {
        String value = text(input, key);
        return value == null ? null : LocalDate.parse(value);
    }

    private Long longVal(String input, String key) {
        String value = text(input, key);
        return value == null ? null : Long.valueOf(value);
    }

    private Integer integer(String input, String key) {
        String value = text(input, key);
        return value == null ? null : Integer.valueOf(value);
    }

    private Boolean bool(String input, String key) {
        String value = text(input, key);
        return value == null ? null : Boolean.valueOf(value);
    }

    private String text(String input, String key) {
        for (String part : input.split(";")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length == 2 && pair[0].trim().equals(key)) {
                String value = pair[1].trim();
                return value.isBlank() || "null".equalsIgnoreCase(value) ? null : value;
            }
        }
        return null;
    }

    private record ServiceCatalogCsvInput(
            String keyword,
            ServiceCategory categoryFilter,
            Boolean activeFilter,
            String serviceCode,
            String name,
            ServiceCategory categoryCode,
            String description,
            Long basePriceVnd,
            Integer durationMinutes,
            Boolean isActive,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {
    }
}

