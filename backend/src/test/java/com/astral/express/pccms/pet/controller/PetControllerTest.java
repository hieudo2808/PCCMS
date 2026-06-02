package com.astral.express.pccms.pet.controller;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.pet.dto.request.UpdatePetRequest;
import com.astral.express.pccms.pet.dto.response.PetResponse;
import com.astral.express.pccms.pet.service.PetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PetControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PetService petService;

    @InjectMocks
    private PetController petController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(petController)
                .setControllerAdvice(new com.astral.express.pccms.common.exception.GlobalExceptionHandler())
                .build();
    }

    @Test
    void should_Return200_when_CustomerUpdatesOwnPet() throws Exception {
        UUID petId = UUID.randomUUID();
        UpdatePetRequest request = new UpdatePetRequest(
                "Milo Updated", null, null, null, null, 12, BigDecimal.valueOf(5),
                null, null, null, null, null
        );

        PetResponse mockResponse = new PetResponse(petId, null, "Milo Updated", null, null, null, null, 12, BigDecimal.valueOf(5), null, null, null, null, null, true, java.util.Collections.emptyList());
        given(petService.updatePet(eq(petId), any(UpdatePetRequest.class))).willReturn(mockResponse);

        mockMvc.perform(put("/api/v1/pets/{petId}", petId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Milo Updated"));
    }

    @Test
    void should_Return403Forbidden_when_CustomerUpdatesOtherPet() throws Exception {
        UUID petId = UUID.randomUUID();
        UpdatePetRequest request = new UpdatePetRequest(
                "Milo Updated", null, null, null, null, 12, BigDecimal.valueOf(5),
                null, null, null, null, null
        );

        // Giả lập Service ném ra lỗi IDOR
        given(petService.updatePet(eq(petId), any(UpdatePetRequest.class)))
                .willThrow(new BusinessException(ErrorCode.ERR_403_FORBIDDEN));

        mockMvc.perform(put("/api/v1/pets/{petId}", petId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_Return403Forbidden_when_CustomerGetsOtherPet() throws Exception {
        UUID petId = UUID.randomUUID();

        // Giả lập Service ném ra lỗi IDOR
        given(petService.getPet(eq(petId)))
                .willThrow(new BusinessException(ErrorCode.ERR_403_FORBIDDEN));

        mockMvc.perform(get("/api/v1/pets/{petId}", petId))
                .andExpect(status().isForbidden());
    }
}
