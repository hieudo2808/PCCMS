package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.dto.response.AvailabilitySummaryResponse;
import com.astral.express.pccms.appointment.dto.response.TimeSlotResponse;
import com.astral.express.pccms.appointment.dto.response.VetOptionResponse;
import com.astral.express.pccms.appointment.entity.ServiceCatalog;
import com.astral.express.pccms.appointment.repository.ServiceCatalogRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AppointmentAvailabilityUseCaseTest {

    @Mock
    private ServiceCatalogRepository serviceCatalogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppointmentResponseAssembler assembler;

    @Mock
    private AppointmentAvailabilityService availabilityService;

    @Mock
    private VetAvailabilityChecker vetAvailabilityChecker;

    @Mock
    private RoomAvailabilityChecker roomAvailabilityChecker;

    @InjectMocks
    private AppointmentAvailabilityUseCase useCase;

    @Test
    void getAvailableSlots_shouldThrowException_whenDateNull() {
        assertThatThrownBy(() -> useCase.getAvailableSlots(null, UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void getAvailableSlots_shouldThrowException_whenDatePast() {
        assertThatThrownBy(() -> useCase.getAvailableSlots(LocalDate.now().minusDays(1), UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_002_PAST_DATETIME);
    }

    @Test
    void getAvailableSlots_shouldReturnSlots() {
        LocalDate date = LocalDate.now().plusDays(1);
        UUID vetId = UUID.randomUUID();
        ServiceCatalog catalog = new ServiceCatalog();
        catalog.setDurationMinutes(60);
        given(serviceCatalogRepository.findByServiceCodeAndIsActiveTrue("MED-GENERAL")).willReturn(Optional.of(catalog));
        given(availabilityService.isSlotAvailable(any(), any(), eq(vetId))).willReturn(true);
        given(assembler.toTimeSlotResponse(any(), any(), eq(true))).willReturn(new TimeSlotResponse(LocalTime.of(8, 0), LocalTime.of(9, 0), "label", true));

        List<TimeSlotResponse> slots = useCase.getAvailableSlots(date, vetId);

        assertThat(slots).isNotEmpty();
    }

    @Test
    void listAvailableVets_shouldReturnEmpty_whenNoVets() {
        given(userRepository.findActiveByRoleCode("VETERINARIAN")).willReturn(List.of());

        List<VetOptionResponse> vets = useCase.listAvailableVets(LocalDate.now(), LocalTime.of(10, 0));

        assertThat(vets).isEmpty();
    }

    @Test
    void listAvailableVets_shouldReturnVets() {
        Users vet = new Users();
        vet.setId(UUID.randomUUID());
        given(userRepository.findActiveByRoleCode("VETERINARIAN")).willReturn(List.of(vet));
        ServiceCatalog catalog = new ServiceCatalog();
        catalog.setDurationMinutes(30);
        given(serviceCatalogRepository.findByServiceCodeAndIsActiveTrue("MED-GENERAL")).willReturn(Optional.of(catalog));
        given(vetAvailabilityChecker.isVetOnDuty(any(), any(), any())).willReturn(true);
        given(vetAvailabilityChecker.isVetFree(any(), any(), any())).willReturn(true);
        given(assembler.toVetOptionResponse(vet, true)).willReturn(new VetOptionResponse(vet.getId(), "Vet 1", true));

        List<VetOptionResponse> vets = useCase.listAvailableVets(LocalDate.now(), LocalTime.of(10, 0));

        assertThat(vets).hasSize(1);
        assertThat(vets.get(0).available()).isTrue();
    }

    @Test
    void listVetsOnDuty_shouldThrowException_whenDateNull() {
        assertThatThrownBy(() -> useCase.listVetsOnDuty(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void listVetsOnDuty_shouldReturnEmpty_whenNoVets() {
        given(userRepository.findActiveByRoleCode("VETERINARIAN")).willReturn(List.of());

        List<VetOptionResponse> vets = useCase.listVetsOnDuty(LocalDate.now());

        assertThat(vets).isEmpty();
    }

    @Test
    void listVetsOnDuty_shouldReturnVets() {
        Users vet = new Users();
        vet.setId(UUID.randomUUID());
        given(userRepository.findActiveByRoleCode("VETERINARIAN")).willReturn(List.of(vet));
        given(vetAvailabilityChecker.findVetIdsOnDutyForDate(any())).willReturn(List.of(vet.getId()));
        given(assembler.toVetOptionResponse(vet, true)).willReturn(new VetOptionResponse(vet.getId(), "Vet 1", true));

        List<VetOptionResponse> vets = useCase.listVetsOnDuty(LocalDate.now());

        assertThat(vets).hasSize(1);
        assertThat(vets.get(0).available()).isTrue();
    }

    @Test
    void getAvailabilitySummary_shouldThrowException_whenDateNull() {
        assertThatThrownBy(() -> useCase.getAvailabilitySummary(null, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void getAvailabilitySummary_shouldReturnSummary() {
        LocalDate date = LocalDate.now().plusDays(1);
        ServiceCatalog catalog = new ServiceCatalog();
        catalog.setDurationMinutes(60);
        given(serviceCatalogRepository.findByServiceCodeAndIsActiveTrue("MED-GENERAL")).willReturn(Optional.of(catalog));
        given(availabilityService.isSlotAvailable(any(), any(), eq(null))).willReturn(true);
        given(assembler.toTimeSlotResponse(any(), any(), eq(true))).willReturn(new TimeSlotResponse(LocalTime.of(8, 0), LocalTime.of(9, 0), "label", true));
        given(roomAvailabilityChecker.getTotalActiveRooms()).willReturn(5);

        Users vet = new Users();
        vet.setId(UUID.randomUUID());
        given(userRepository.findActiveByRoleCode("VETERINARIAN")).willReturn(List.of(vet));
        given(vetAvailabilityChecker.findVetIdsOnDutyForDate(any())).willReturn(List.of(vet.getId()));
        given(assembler.toVetOptionResponse(vet, true)).willReturn(new VetOptionResponse(vet.getId(), "Vet 1", true));

        AvailabilitySummaryResponse response = useCase.getAvailabilitySummary(date, null);

        assertThat(response.totalExamRooms()).isEqualTo(5);
        assertThat(response.vetsOnDuty()).isEqualTo(1);
        assertThat(response.totalSlots()).isGreaterThan(0);
        assertThat(response.availableSlots()).isGreaterThan(0);
        assertThat(response.freeRoomsForSlot()).isNull();
    }

    @Test
    void getAvailabilitySummary_shouldReturnSummary_withSlotStart() {
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime slotStart = LocalTime.of(10, 0);
        ServiceCatalog catalog = new ServiceCatalog();
        catalog.setDurationMinutes(60);
        given(serviceCatalogRepository.findByServiceCodeAndIsActiveTrue("MED-GENERAL")).willReturn(Optional.of(catalog));
        given(availabilityService.isSlotAvailable(any(), any(), eq(null))).willReturn(true);
        given(assembler.toTimeSlotResponse(any(), any(), eq(true))).willReturn(new TimeSlotResponse(LocalTime.of(8, 0), LocalTime.of(9, 0), "label", true));
        given(roomAvailabilityChecker.getTotalActiveRooms()).willReturn(5);

        Users vet = new Users();
        vet.setId(UUID.randomUUID());
        given(userRepository.findActiveByRoleCode("VETERINARIAN")).willReturn(List.of(vet));
        given(vetAvailabilityChecker.findVetIdsOnDutyForDate(any())).willReturn(List.of(vet.getId()));
        given(assembler.toVetOptionResponse(vet, true)).willReturn(new VetOptionResponse(vet.getId(), "Vet 1", true));
        given(roomAvailabilityChecker.countFreeRooms(any(), any())).willReturn(3L);
        given(vetAvailabilityChecker.isVetOnDuty(any(), any(), any())).willReturn(true);
        given(vetAvailabilityChecker.isVetFree(any(), any(), any())).willReturn(true);

        AvailabilitySummaryResponse response = useCase.getAvailabilitySummary(date, slotStart);

        assertThat(response.freeRoomsForSlot()).isEqualTo(3);
        assertThat(response.freeVetsForSlot()).isEqualTo(1);
    }
}
