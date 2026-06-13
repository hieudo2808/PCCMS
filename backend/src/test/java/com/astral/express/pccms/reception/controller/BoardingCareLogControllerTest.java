package com.astral.express.pccms.reception.controller;

import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.reception.service.BoardingCareLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BoardingCareLogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BoardingCareLogService boardingCareLogService;

    @InjectMocks
    private BoardingCareLogController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listBookings_success() throws Exception {
        given(boardingCareLogService.listBookings(any(), any())).willReturn(List.of());

        mockMvc.perform(get("/v1/reception/boarding/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listCareLogs_success() throws Exception {
        given(boardingCareLogService.listCareLogs(any(), any())).willReturn(List.of());

        mockMvc.perform(get("/v1/reception/boarding/care-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void saveCareLog_success() throws Exception {
        mockMvc.perform(post("/v1/reception/boarding/care-logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"periodCode\":\"MORNING\",\"feedingStatus\":\"Good\",\"hygieneStatus\":\"Clean\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void uploadMedia_success() throws Exception {
        UUID id = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());

        mockMvc.perform(multipart("/v1/reception/boarding/care-logs/{id}/media", id)
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
