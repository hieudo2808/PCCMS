package com.astral.express.pccms.appointment.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
class AppointmentSlotsIntegrationTest {

    @Autowired
    private AppointmentService appointmentService;

    @Test
    void should_ReturnSlots_when_FutureDate() {
        assertThatCode(() -> appointmentService.getAvailableSlots(LocalDate.of(2026, 6, 8), null))
                .doesNotThrowAnyException();
    }
}
