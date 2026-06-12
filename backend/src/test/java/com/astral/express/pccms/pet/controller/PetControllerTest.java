package com.astral.express.pccms.pet.controller;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.pet.dto.request.UpdatePetRequest;
import com.astral.express.pccms.pet.dto.response.PetResponse;
import com.astral.express.pccms.pet.entity.PetSex;
import com.astral.express.pccms.pet.service.PetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new com.astral.express.pccms.common.exception.GlobalExceptionHandler())
                .build();
    }

    @Test
    void should_ListPetsWithCurrentUserScope_whenOwnerIdIsOmitted() throws Exception {
        given(petService.listPets(isNull(), any(Pageable.class)))
                .willReturn(PageResponse.of(new PageImpl<>(List.of())));

        mockMvc.perform(get("/v1/pets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(petService).listPets(isNull(), any(Pageable.class));
    }

    @Test
    void should_Return200_when_CustomerUpdatesOwnPet() throws Exception {
        UUID petId = UUID.randomUUID();
        UUID speciesId = UUID.randomUUID();
        UpdatePetRequest request = new UpdatePetRequest(
                "Milo Updated", speciesId, null, PetSex.MALE, null, 12, BigDecimal.valueOf(5),
                null, null, null, null, null
        );

        PetResponse mockResponse = new PetResponse(
                petId, null, "Milo Updated", speciesId, "ChÃ³", null, null, PetSex.MALE, null, 12,
                BigDecimal.valueOf(5), null, null, null, null, null, true);
        given(petService.updatePet(eq(petId), any(UpdatePetRequest.class))).willReturn(mockResponse);

        mockMvc.perform(put("/v1/pets/{petId}", petId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Milo Updated"));
    }

    @Test
    void should_Return403Forbidden_when_CustomerUpdatesOtherPet() throws Exception {
        UUID petId = UUID.randomUUID();
        UUID speciesId = UUID.randomUUID();
        UpdatePetRequest request = new UpdatePetRequest(
                "Milo Updated", speciesId, null, PetSex.MALE, null, 12, BigDecimal.valueOf(5),
                null, null, null, null, null
        );

        given(petService.updatePet(eq(petId), any(UpdatePetRequest.class)))
                .willThrow(new BusinessException(ErrorCode.ERR_403_FORBIDDEN));

        mockMvc.perform(put("/v1/pets/{petId}", petId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void should_Return403Forbidden_when_CustomerGetsOtherPet() throws Exception {
        UUID petId = UUID.randomUUID();

        // Giáº£ láº­p Service nÃ©m ra lá»—i IDOR
        given(petService.getPet(eq(petId)))
                .willThrow(new BusinessException(ErrorCode.ERR_403_FORBIDDEN));

        mockMvc.perform(get("/v1/pets/{petId}", petId))
                .andExpect(status().isForbidden());
    }
}
