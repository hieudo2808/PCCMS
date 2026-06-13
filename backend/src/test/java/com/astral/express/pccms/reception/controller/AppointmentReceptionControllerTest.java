package com.astral.express.pccms.reception.controller;

import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.reception.service.AppointmentReceptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
                .build();
    }

    @Test
    void listAppointments_success() throws Exception {
        given(appointmentReceptionService.listAppointments(any(), any())).willReturn(List.of());

        mockMvc.perform(get("/v1/reception/appointments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void quickCreateAndReceive_success() throws Exception {
        mockMvc.perform(post("/v1/reception/appointments/quick")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"0123456789\",\"ownerName\":\"Test Owner\",\"petName\":\"Test Pet\",\"symptomText\":\"Test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void receive_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/v1/reception/appointments/{id}/receive", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void cancel_success() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(patch("/v1/reception/appointments/{id}/cancel", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
