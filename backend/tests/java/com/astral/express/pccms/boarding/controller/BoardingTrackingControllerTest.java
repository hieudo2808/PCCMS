package com.astral.express.pccms.boarding.controller;

import com.astral.express.pccms.boarding.dto.response.BoardingStayResponse;
import com.astral.express.pccms.boarding.dto.response.CareLogResponse;
import com.astral.express.pccms.boarding.service.BoardingTrackingService;
import com.astral.express.pccms.identity.security.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BoardingTrackingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BoardingTrackingService boardingTrackingService;

    @Mock
    private SecurityHelper securityHelper;

    @InjectMocks
    private BoardingTrackingController boardingTrackingController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(boardingTrackingController)
                .setControllerAdvice(new com.astral.express.pccms.common.exception.GlobalExceptionHandler())
                .build();
    }

    @Test
    void should_ReturnActiveStays() throws Exception {
        UUID ownerId = UUID.randomUUID();
        given(securityHelper.getCurrentUserId()).willReturn(ownerId);
        given(boardingTrackingService.listActiveStays(ownerId)).willReturn(List.of(
                new BoardingStayResponse(UUID.randomUUID(), "Milu", "Chó", "Poodle")
        ));

        mockMvc.perform(get("/v1/boarding/owner/stays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].petName").value("Milu"));
    }

    @Test
    void should_ReturnCareLogs() throws Exception {
        UUID ownerId = UUID.randomUUID();
        given(securityHelper.getCurrentUserId()).willReturn(ownerId);
        given(boardingTrackingService.listCareLogs(ownerId, null)).willReturn(List.of(
                new CareLogResponse(
                        UUID.randomUUID(), UUID.randomUUID(), "Milu",
                        LocalDate.of(2026, 6, 5), "MORNING", "Sáng",
                        "Ăn tốt", "Bình thường", null, "Ghi chú", List.of()
                )
        ));

        mockMvc.perform(get("/v1/boarding/owner/care-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].feedingStatus").value("Ăn tốt"));
    }
}
