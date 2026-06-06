package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.dto.request.CreateMedicalAppointmentRequest;
import com.astral.express.pccms.appointment.entity.*;
import com.astral.express.pccms.appointment.repository.*;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private ServiceOrderRepository serviceOrderRepository;
    @Mock private ServiceCatalogRepository serviceCatalogRepository;
    @Mock private ReceptionTicketRepository receptionTicketRepository;
    @Mock private ExamRoomRepository examRoomRepository;
    @Mock private WorkScheduleRepository workScheduleRepository;
    @Mock private GroomingStationRepository groomingStationRepository;
    @Mock private GroomingTicketRepository groomingTicketRepository;
    @Mock private RoomTypeRepository roomTypeRepository;
    @Mock private BoardingBookingRepository boardingBookingRepository;
    @Mock private PetRepository petRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    @Test
    void should_ThrowPastDateError_when_BookingInPast() {
        UUID ownerId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        Pets pet = buildPet(petId, ownerId);

        given(petRepository.findById(petId)).willReturn(Optional.of(pet));

        CreateMedicalAppointmentRequest request = new CreateMedicalAppointmentRequest(
                petId,
                LocalDate.now().minusDays(1),
                LocalTime.of(9, 0),
                null,
                "Nôn mửa",
                null
        );

        assertThatThrownBy(() -> appointmentService.createMedicalAppointment(request, ownerId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ERR_APT_002_PAST_DATETIME);
    }

    @Test
    void should_CreatePendingAppointment_when_ValidBooking() {
        UUID ownerId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        UUID vetId = UUID.randomUUID();
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        Pets pet = buildPet(petId, ownerId);
        Users vet = buildVet(vetId);
        ServiceCatalog service = buildMedicalService();
        ExamRoom room = mock(ExamRoom.class);
        UUID roomId = UUID.randomUUID();
        when(room.getId()).thenReturn(roomId);

        given(petRepository.findById(petId)).willReturn(Optional.of(pet));
        given(serviceCatalogRepository.findByServiceCodeAndIsActiveTrue("MED-GENERAL")).willReturn(Optional.of(service));
        given(examRoomRepository.findByIsActiveTrueOrderByRoomCodeAsc()).willReturn(List.of(room));
        given(appointmentRepository.countOverlappingInRoom(eq(roomId), any(), any())).willReturn(0L);
        given(workScheduleRepository.findAvailableVetIds(tomorrow, LocalTime.of(9, 0))).willReturn(List.of());
        given(userRepository.findActiveByRoleCode("VETERINARIAN")).willReturn(List.of(vet));
        given(userRepository.findById(vetId)).willReturn(Optional.of(vet));
        given(appointmentRepository.countOverlappingForStaff(eq(vetId), any(), any())).willReturn(0L);
        given(serviceOrderRepository.maxAppointmentOrderSequence()).willReturn(0L);
        given(appointmentRepository.save(any(Appointment.class))).willAnswer(inv -> {
            Appointment a = inv.getArgument(0);
            a.setId(UUID.randomUUID());
            return a;
        });

        CreateMedicalAppointmentRequest request = new CreateMedicalAppointmentRequest(
                petId, tomorrow, LocalTime.of(9, 0), vetId, "Bỏ ăn 2 ngày", "Thú nhát"
        );

        var response = appointmentService.createMedicalAppointment(request, ownerId);

        assertThat(response.statusCode()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(response.statusLabel()).isEqualTo("Chờ tiếp nhận");
        assertThat(response.petName()).isEqualTo("Milu");

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatusCode()).isEqualTo(AppointmentStatus.PENDING);
    }

    private Pets buildPet(UUID petId, UUID ownerId) {
        Users owner = Users.builder().id(ownerId).fullName("Nguyễn Minh").build();
        return Pets.builder().id(petId).owner(owner).name("Milu").isActive(true).build();
    }

    private Users buildVet(UUID vetId) {
        Roles role = Roles.builder().code("VETERINARIAN").build();
        return Users.builder().id(vetId).fullName("Trần Văn A").role(role).build();
    }

    private ServiceCatalog buildMedicalService() {
        ServiceCatalog service = new ServiceCatalog();
        service.setId(UUID.randomUUID());
        service.setServiceCode("MED-GENERAL");
        service.setBasePriceVnd(BigDecimal.valueOf(200000));
        service.setDurationMinutes(30);
        service.setCategoryCode(ServiceCategory.MEDICAL);
        service.setIsActive(true);
        return service;
    }

}
