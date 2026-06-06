package com.astral.express.pccms.boarding.controller;

import com.astral.express.pccms.boarding.dto.response.StaffBoardingStayResponse;
import com.astral.express.pccms.boarding.service.BoardingStaffService;
import com.astral.express.pccms.identity.security.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BoardingStaffControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BoardingStaffService boardingStaffService;

    @Mock
    private SecurityHelper securityHelper;

    @InjectMocks
    private BoardingStaffController boardingStaffController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(boardingStaffController)
                .setControllerAdvice(new com.astral.express.pccms.common.exception.GlobalExceptionHandler())
                .build();
    }

    @Test
    void should_ReturnStaffActiveStays() throws Exception {
        UUID sessionId = UUID.randomUUID();
        given(boardingStaffService.listActiveStays()).willReturn(List.of(
                new StaffBoardingStayResponse(
                        sessionId, UUID.randomUUID(), "Milu", "STANDARD",
                        2, 5, "Đã cập nhật sáng"
                )
        ));

        mockMvc.perform(get("/v1/boarding/staff/stays"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].petName").value("Milu"))
                .andExpect(jsonPath("$.data[0].todayLogSummary").value("Đã cập nhật sáng"));
    }
}
