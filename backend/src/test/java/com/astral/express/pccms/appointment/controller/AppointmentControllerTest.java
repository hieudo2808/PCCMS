package com.astral.express.pccms.appointment.controller;

import com.astral.express.pccms.appointment.dto.response.AppointmentResponse;
import com.astral.express.pccms.appointment.entity.AppointmentStatus;
import com.astral.express.pccms.appointment.entity.AppointmentType;
import com.astral.express.pccms.appointment.service.AppointmentAvailabilityUseCase;
import com.astral.express.pccms.appointment.service.AppointmentLifecycleUseCase;
import com.astral.express.pccms.appointment.service.AppointmentQueryUseCase;
import com.astral.express.pccms.appointment.service.AppointmentResponseAssembler;
import com.astral.express.pccms.appointment.service.BoardingBookingUseCase;
import com.astral.express.pccms.appointment.service.CreateMedicalAppointmentUseCase;
import com.astral.express.pccms.appointment.service.GroomingBookingUseCase;
import com.astral.express.pccms.appointment.service.QuickCheckInUseCase;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.identity.security.SecurityContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AppointmentAvailabilityUseCase availabilityUseCase;
    @Mock
    private AppointmentLifecycleUseCase lifecycleUseCase;
    @Mock
    private AppointmentQueryUseCase queryUseCase;
    @Mock
    private BoardingBookingUseCase boardingBookingUseCase;
    @Mock
    private GroomingBookingUseCase groomingBookingUseCase;
    @Mock
    private CreateMedicalAppointmentUseCase createMedicalAppointmentUseCase;
    @Mock
    private QuickCheckInUseCase quickCheckInUseCase;
    @Mock
    private AppointmentResponseAssembler appointmentResponseAssembler;

    @Mock
    private SecurityContextService SecurityContextService;

    @InjectMocks
    private AppointmentController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void should_ReturnExactJsonShape_When_GetTodayAppointments() throws Exception {
        UUID appointmentId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        UUID vetId = UUID.randomUUID();
        
        OffsetDateTime start = OffsetDateTime.parse("2026-06-11T08:00:00Z");
        OffsetDateTime end = OffsetDateTime.parse("2026-06-11T08:30:00Z");

        AppointmentResponse mockedResponse = new AppointmentResponse(
                appointmentId,
                "AP0001",
                AppointmentType.MEDICAL,
                "Khám Tổng Quát",
                start,
                end,
                "Nguyễn Văn A",
                "0901234567",
                petId,
                "Milu",
                vetId,
                "BS. Lê Văn B",
                AppointmentStatus.CHECKED_IN,
                "Đã tiếp nhận",
                "Sốt",
                "Ghi chú",
                5
        );

        given(queryUseCase.listTodayAppointments(any(), any(), any(), any()))
                .willReturn(java.util.List.of(mockedResponse));

        mockMvc.perform(get("/v1/appointments/today")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(appointmentId.toString()))
                .andExpect(jsonPath("$.data[0].appointmentCode").value("AP0001"))
                .andExpect(jsonPath("$.data[0].appointmentType").value("MEDICAL"))
                .andExpect(jsonPath("$.data[0].serviceName").value("Khám Tổng Quát"))
                .andExpect(jsonPath("$.data[0].ownerName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.data[0].petName").value("Milu"))
                .andExpect(jsonPath("$.data[0].assignedVetName").value("BS. Lê Văn B"))
                .andExpect(jsonPath("$.data[0].statusCode").value("CHECKED_IN"))
                .andExpect(jsonPath("$.data[0].queueNumber").value(5));
    }
}
