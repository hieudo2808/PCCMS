package com.astral.express.pccms.reception.controller;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.reception.dto.request.AppointmentCancelRequest;
import com.astral.express.pccms.reception.dto.request.AppointmentReceiveRequest;
import com.astral.express.pccms.reception.dto.request.QuickAppointmentRequest;
import com.astral.express.pccms.reception.dto.response.AppointmentReceptionResponse;
import com.astral.express.pccms.reception.service.AppointmentReceptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AppointmentReceptionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AppointmentReceptionService appointmentReceptionService;

    @InjectMocks
    private AppointmentReceptionController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .defaultRequest(get("/").contextPath("/api"))
                .build();
    }

    @Test
    @DisplayName("API-REC-001: List appointments successfully")
    void should_return_list_of_appointments() throws Exception {
        UUID id = UUID.randomUUID();
        AppointmentReceptionResponse response = new AppointmentReceptionResponse(
                id, "PENDING", new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()),
                "Symptom", "SO-123", "Owner", "0123456789", "Pet", "Doctor", "Medical"
        );

        given(appointmentReceptionService.listAppointments(anyString(), isNull())).willReturn(List.of(response));

        mockMvc.perform(get("/api/v1/reception/appointments")
                        .contextPath("/api")
                        .param("q", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Lấy danh sách lịch hẹn thành công"))
                .andExpect(jsonPath("$.data[0].id").value(id.toString()))
                .andExpect(jsonPath("$.data[0].statusCode").value("PENDING"));

        verify(appointmentReceptionService).listAppointments("", null);
    }

    @Test
    @DisplayName("API-REC-002: Quick check-in successfully")
    void should_create_and_receive_quick_appointment_successfully() throws Exception {
        UUID id = UUID.randomUUID();
        AppointmentReceptionResponse response = new AppointmentReceptionResponse(
                id, "CHECKED_IN", new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()),
                "Vomiting", "SO-123", "John Doe", "0987654321", "Rex", null, "MED-GENERAL"
        );

        given(appointmentReceptionService.quickCreateAndReceive(any(QuickAppointmentRequest.class)))
                .willReturn(response);

        String payload = """
                {
                  "phone": "0987654321",
                  "ownerName": "John Doe",
                  "petName": "Rex",
                  "symptomText": "Vomiting",
                  "serviceCode": "MED-GENERAL"
                }
                """;

        mockMvc.perform(post("/api/v1/reception/appointments/quick")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Tạo nhanh và tiếp nhận thành công"))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.statusCode").value("CHECKED_IN"));

        verify(appointmentReceptionService).quickCreateAndReceive(any(QuickAppointmentRequest.class));
    }

    @Test
    @DisplayName("API-REC-003: Quick check-in fails on validation")
    void should_return_400_when_quick_appointment_validation_fails() throws Exception {
        String payload = """
                {
                  "ownerName": "John Doe",
                  "symptomText": "Vomiting"
                }
                """; // missing phone and petName

        mockMvc.perform(post("/api/v1/reception/appointments/quick")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors").exists());

        verifyNoInteractions(appointmentReceptionService);
    }

    @Test
    @DisplayName("API-REC-004: Receive appointment successfully")
    void should_receive_appointment_successfully() throws Exception {
        UUID id = UUID.randomUUID();
        AppointmentReceptionResponse response = new AppointmentReceptionResponse(
                id, "CHECKED_IN", new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()),
                "Symptom", "SO-123", "Owner", "0123456789", "Pet", "Doctor", "Medical"
        );

        given(appointmentReceptionService.receive(eq(id), any(AppointmentReceiveRequest.class)))
                .willReturn(response);

        String payload = """
                {
                  "note": "Arrived early"
                }
                """;

        mockMvc.perform(patch("/api/v1/reception/appointments/{id}/receive", id)
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Tiếp nhận thành công"))
                .andExpect(jsonPath("$.data.statusCode").value("CHECKED_IN"));

        verify(appointmentReceptionService).receive(eq(id), any(AppointmentReceiveRequest.class));
    }

    @Test
    @DisplayName("API-REC-005: Receive appointment fails - Not found")
    void should_return_404_when_receiving_non_existent_appointment() throws Exception {
        UUID id = UUID.randomUUID();
        given(appointmentReceptionService.receive(eq(id), any()))
                .willThrow(new BusinessException(ErrorCode.ERR_REC_001_APPOINTMENT_NOT_FOUND));

        mockMvc.perform(patch("/api/v1/reception/appointments/{id}/receive", id)
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.errorCode").value("ERR_REC_001_APPOINTMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("API-REC-006: Receive appointment fails - Not receivable")
    void should_return_400_when_appointment_not_receivable() throws Exception {
        UUID id = UUID.randomUUID();
        given(appointmentReceptionService.receive(eq(id), any()))
                .willThrow(new BusinessException(ErrorCode.ERR_REC_002_APPOINTMENT_NOT_RECEIVABLE));

        mockMvc.perform(patch("/api/v1/reception/appointments/{id}/receive", id)
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errorCode").value("ERR_REC_002_APPOINTMENT_NOT_RECEIVABLE"));
    }

    @Test
    @DisplayName("API-REC-007: Cancel appointment successfully")
    void should_cancel_appointment_successfully() throws Exception {
        UUID id = UUID.randomUUID();
        AppointmentReceptionResponse response = new AppointmentReceptionResponse(
                id, "CANCELLED", new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()),
                "Symptom", "SO-123", "Owner", "0123456789", "Pet", "Doctor", "Medical"
        );

        given(appointmentReceptionService.cancel(eq(id), any(AppointmentCancelRequest.class)))
                .willReturn(response);

        String payload = """
                {
                  "reason": "Customer no show"
                }
                """;

        mockMvc.perform(patch("/api/v1/reception/appointments/{id}/cancel", id)
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Hủy lịch hẹn thành công"))
                .andExpect(jsonPath("$.data.statusCode").value("CANCELLED"));

        verify(appointmentReceptionService).cancel(eq(id), any(AppointmentCancelRequest.class));
    }
}
