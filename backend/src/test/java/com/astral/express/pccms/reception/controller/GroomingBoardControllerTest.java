package com.astral.express.pccms.reception.controller;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.reception.dto.request.GroomingStatusUpdateRequest;
import com.astral.express.pccms.reception.dto.response.GroomingTicketResponse;
import com.astral.express.pccms.reception.service.GroomingBoardService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GroomingBoardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GroomingBoardService groomingBoardService;

    @InjectMocks
    private GroomingBoardController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .defaultRequest(get("/").contextPath("/api"))
                .build();
    }

    @Test
    @DisplayName("API-REC-014: List grooming tickets successfully")
    void should_return_list_of_grooming_tickets() throws Exception {
        UUID id = UUID.randomUUID();
        GroomingTicketResponse response = new GroomingTicketResponse(
                id, "PENDING", null, null, "Note", null, UUID.randomUUID(),
                new Timestamp(System.currentTimeMillis()), "SO-123", "Rex", "Owner", "0123456789", "Spa", "SPA01"
        );

        given(groomingBoardService.listTickets(anyString(), isNull())).willReturn(List.of(response));

        mockMvc.perform(get("/api/v1/reception/grooming-tickets")
                        .contextPath("/api")
                        .param("q", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Lấy bảng dịch vụ làm đẹp thành công"))
                .andExpect(jsonPath("$.data[0].id").value(id.toString()))
                .andExpect(jsonPath("$.data[0].statusCode").value("PENDING"));

        verify(groomingBoardService).listTickets("", null);
    }

    @Test
    @DisplayName("API-REC-015: Update grooming status successfully")
    void should_update_grooming_status_successfully() throws Exception {
        UUID id = UUID.randomUUID();
        GroomingTicketResponse response = new GroomingTicketResponse(
                id, "IN_SERVICE", new Timestamp(System.currentTimeMillis()), null, "Note", null, UUID.randomUUID(),
                new Timestamp(System.currentTimeMillis()), "SO-123", "Rex", "Owner", "0123456789", "Spa", "SPA01"
        );

        given(groomingBoardService.updateStatus(eq(id), any(GroomingStatusUpdateRequest.class)))
                .willReturn(response);

        String payload = """
                {
                  "statusCode": "IN_SERVICE"
                }
                """;

        mockMvc.perform(patch("/api/v1/reception/grooming-tickets/{id}/status", id)
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Cập nhật trạng thái làm đẹp thành công"))
                .andExpect(jsonPath("$.data.statusCode").value("IN_SERVICE"));

        verify(groomingBoardService).updateStatus(eq(id), any(GroomingStatusUpdateRequest.class));
    }

    @Test
    @DisplayName("API-REC-016: Update grooming status fails on validation")
    void should_return_400_when_grooming_status_validation_fails() throws Exception {
        UUID id = UUID.randomUUID();
        String payload = """
                {
                  "statusCode": "UNKNOWN_STATUS"
                }
                """; // invalid status code regex

        mockMvc.perform(patch("/api/v1/reception/grooming-tickets/{id}/status", id)
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors").exists());

        verifyNoInteractions(groomingBoardService);
    }

    @Test
    @DisplayName("API-REC-017: Update grooming status fails - Not found")
    void should_return_404_when_updating_status_of_non_existent_grooming_ticket() throws Exception {
        UUID id = UUID.randomUUID();
        given(groomingBoardService.updateStatus(eq(id), any(GroomingStatusUpdateRequest.class)))
                .willThrow(new BusinessException(ErrorCode.ERR_REC_006_GROOMING_TICKET_NOT_FOUND));

        String payload = """
                {
                  "statusCode": "IN_SERVICE"
                }
                """;

        mockMvc.perform(patch("/api/v1/reception/grooming-tickets/{id}/status", id)
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.errorCode").value("ERR_REC_006_GROOMING_TICKET_NOT_FOUND"));
    }

    @Test
    @DisplayName("API-REC-018: Update grooming status fails - Invalid transition")
    void should_return_400_when_grooming_status_transition_is_invalid() throws Exception {
        UUID id = UUID.randomUUID();
        given(groomingBoardService.updateStatus(eq(id), any(GroomingStatusUpdateRequest.class)))
                .willThrow(new BusinessException(ErrorCode.ERR_REC_007_INVALID_GROOMING_STATUS_TRANSITION));

        String payload = """
                {
                  "statusCode": "IN_SERVICE"
                }
                """;

        mockMvc.perform(patch("/api/v1/reception/grooming-tickets/{id}/status", id)
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errorCode").value("ERR_REC_007_INVALID_GROOMING_STATUS_TRANSITION"));
    }
}
