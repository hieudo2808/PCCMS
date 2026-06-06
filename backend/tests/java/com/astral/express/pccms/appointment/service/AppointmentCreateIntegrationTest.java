package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.dto.request.CreateMedicalAppointmentRequest;
import com.astral.express.pccms.appointment.entity.AppointmentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
class AppointmentCreateIntegrationTest {

    @Autowired
    private AppointmentService appointmentService;

    @Test
    void should_CreateAppointment_when_ValidOwnerPet() {
        UUID ownerId = UUID.fromString("16e7d095-2013-4f2e-aafc-440e122eaaa3");
        UUID petId = UUID.fromString("69b8af2b-8e8c-4e57-b0f5-f23833cf4a26");
        LocalDate date = LocalDate.now().plusDays(3);

        CreateMedicalAppointmentRequest request = new CreateMedicalAppointmentRequest(
                petId,
                date,
                LocalTime.of(10, 0),
                null,
                "Thử nghiệm đặt lịch",
                null
        );

        assertThatCode(() -> {
            var response = appointmentService.createMedicalAppointment(request, ownerId);
            assertThat(response.statusCode()).isEqualTo(AppointmentStatus.PENDING);
        }).doesNotThrowAnyException();
    }
}
