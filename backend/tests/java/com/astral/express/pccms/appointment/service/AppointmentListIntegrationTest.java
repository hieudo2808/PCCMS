package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.dto.response.AppointmentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
class AppointmentListIntegrationTest {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    private AppointmentService appointmentService;

    @Test
    void should_ListTodayAppointments_without_FilterError() {
        LocalDate date = LocalDate.now(CLINIC_ZONE);

        assertThatCode(() -> {
            List<AppointmentResponse> list = appointmentService.listTodayAppointments(date, null, null, null);
            assertThat(list).isNotNull();
        }).doesNotThrowAnyException();
    }

    @Test
    void should_ListTodayAppointments_with_NameFilter() {
        LocalDate date = LocalDate.of(2026, 6, 6);

        List<AppointmentResponse> list = appointmentService.listTodayAppointments(date, null, null, "Thắng");

        assertThat(list).isNotEmpty();
    }
}
