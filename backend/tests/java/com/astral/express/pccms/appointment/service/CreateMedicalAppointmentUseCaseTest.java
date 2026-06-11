package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.dto.request.CreateMedicalAppointmentRequest;
import com.astral.express.pccms.appointment.entity.*;
import com.astral.express.pccms.appointment.repository.*;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.user.entity.Users;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateMedicalAppointmentUseCaseTest {

    @Mock private PetRepository petRepository;
    @Mock private ServiceCatalogRepository serviceCatalogRepository;
    @Mock private ServiceOrderRepository serviceOrderRepository;
    @Mock private AppointmentRepository appointmentRepository;
    @Mock private AppointmentAvailabilityService availabilityService;
    @Mock private VetAvailabilityChecker vetAvailabilityChecker;
    @Mock private RoomAvailabilityChecker roomAvailabilityChecker;
    @Mock private ServiceOrderFactory serviceOrderFactory;
    @Mock private PetValidationService petValidationService;

    @InjectMocks
    private CreateMedicalAppointmentUseCase createMedicalAppointmentUseCase;

    @ParameterizedTest
    @CsvFileSource(resources = "/testcases/appointment-service-testcases.csv", numLinesToSkip = 1)
    void should_CreateMedicalAppointment_OrThrowException_BasedOnScenario(String ruleId, String caseId, String scenario, int dateOffsetDays, String timeStr, String expectedErrorCode) {
        UUID ownerId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        UUID vetId = UUID.randomUUID();
        LocalDate appointmentDate = LocalDate.now().plusDays(dateOffsetDays);
        LocalTime appointmentTime = LocalTime.parse(timeStr);

        Pets pet = buildPet(petId, ownerId);
        lenient().when(petValidationService.findPetOwnedBy(petId, ownerId)).thenReturn(pet);
        lenient().when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

        CreateMedicalAppointmentRequest request = new CreateMedicalAppointmentRequest(
                petId, appointmentDate, appointmentTime, vetId, "Bỏ ăn 2 ngày", "Thú nhát"
        );

        if ("SUCCESS".equals(expectedErrorCode)) {
            ServiceCatalog service = new ServiceCatalog();
            service.setId(UUID.randomUUID());
            service.setServiceCode("MED-GENERAL");
            service.setDurationMinutes(30);

            when(serviceCatalogRepository.findByServiceCodeAndIsActiveTrue(anyString())).thenReturn(Optional.of(service));
            when(availabilityService.isSlotAvailable(any(), any(), nullable(UUID.class))).thenReturn(true);
            when(vetAvailabilityChecker.requireVetAvailable(any(), any(), nullable(UUID.class), any(), any())).thenReturn(new Users());
            when(roomAvailabilityChecker.requireRoomAvailable(any(), any())).thenReturn(new ExamRoom());
            when(serviceOrderFactory.createServiceOrder(any(), any(), any(), any(), any(), any())).thenReturn(new ServiceOrder());
            
            when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> {
                Appointment a = inv.getArgument(0);
                a.setId(UUID.randomUUID());
                return a;
            });

            var response = createMedicalAppointmentUseCase.createMedicalAppointment(request, ownerId);

            assertThat(response.getStatusCode()).isEqualTo(AppointmentStatus.PENDING);

            ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
            verify(appointmentRepository).save(captor.capture());
            verify(serviceOrderRepository).save(any(ServiceOrder.class));
            assertThat(captor.getValue().getStatusCode()).isEqualTo(AppointmentStatus.PENDING);
        } else {
            lenient().when(availabilityService.isSlotAvailable(any(), any(), nullable(UUID.class)))
                .thenThrow(new BusinessException(ErrorCode.valueOf(expectedErrorCode)));

            if ("PET_NOT_FOUND".equals(expectedErrorCode) || "PET_INACTIVE".equals(expectedErrorCode) || "PET_DECEASED".equals(expectedErrorCode)) {
                 lenient().when(petValidationService.findPetOwnedBy(petId, ownerId))
                    .thenThrow(new BusinessException(ErrorCode.valueOf(expectedErrorCode)));
            }
            if ("SERVICE_NOT_FOUND".equals(expectedErrorCode)) {
                 lenient().when(serviceCatalogRepository.findByServiceCodeAndIsActiveTrue(anyString()))
                    .thenThrow(new BusinessException(ErrorCode.valueOf(expectedErrorCode)));
                 lenient().when(availabilityService.isSlotAvailable(any(), any(), nullable(UUID.class))).thenReturn(true);
                 lenient().when(vetAvailabilityChecker.requireVetAvailable(any(), any(), nullable(UUID.class), any(), any())).thenReturn(new Users());
                 lenient().when(roomAvailabilityChecker.requireRoomAvailable(any(), any())).thenReturn(new ExamRoom());
            }

            assertThatThrownBy(() -> createMedicalAppointmentUseCase.createMedicalAppointment(request, ownerId))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.valueOf(expectedErrorCode));
        }
    }

    private Pets buildPet(UUID petId, UUID ownerId) {
        Users owner = Users.builder().id(ownerId).fullName("Nguyễn Minh").build();
        return Pets.builder().id(petId).owner(owner).name("Milu").isActive(true).build();
    }
}
