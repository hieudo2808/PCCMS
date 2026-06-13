package com.astral.express.pccms.pet.controller;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.pet.dto.request.CreatePetRequest;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PetControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PetService petService;

    @InjectMocks
    private PetController petController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        mockMvc = MockMvcBuilders.standaloneSetup(petController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private PetResponse mockPetResponse() {
        return new PetResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Rex",
                UUID.randomUUID(),
                "Dog",
                UUID.randomUUID(),
                "Bulldog",
                PetSex.MALE,
                LocalDate.of(2020, 1, 1),
                36,
                new BigDecimal("15.50"),
                "Brown",
                "None",
                "None",
                "None",
                "None",
                true
        );
    }

    @Test
    void createPet_success() throws Exception {
        given(petService.createPet(any(CreatePetRequest.class))).willReturn(mockPetResponse());

        mockMvc.perform(post("/v1/pets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rex\",\"speciesId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"breedId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"sex\":\"MALE\",\"birthDate\":\"2020-01-01\",\"weightKg\":15.50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Rex"));
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
    void listPets_withOwnerId_success() throws Exception {
        UUID ownerId = UUID.randomUUID();
        PageResponse<PetResponse> pageResponse = PageResponse.of(new PageImpl<>(List.of(mockPetResponse())));
        given(petService.listPets(eq(ownerId), any(), any())).willReturn(pageResponse);

        mockMvc.perform(get("/v1/pets")
                        .param("ownerId", ownerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getPet_success() throws Exception {
        UUID petId = UUID.randomUUID();
        given(petService.getPet(petId)).willReturn(mockPetResponse());

        mockMvc.perform(get("/v1/pets/{petId}", petId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Rex"));
    }

    @Test
    void should_Return403Forbidden_when_CustomerGetsOtherPet() throws Exception {
        UUID petId = UUID.randomUUID();

        given(petService.getPet(eq(petId)))
                .willThrow(new BusinessException(ErrorCode.ERR_403_FORBIDDEN));

        mockMvc.perform(get("/v1/pets/{petId}", petId))
                .andExpect(status().isForbidden());
    }

    @Test
    void updatePet_success() throws Exception {
        UUID petId = UUID.randomUUID();
        given(petService.updatePet(eq(petId), any(UpdatePetRequest.class))).willReturn(mockPetResponse());

        mockMvc.perform(put("/v1/pets/{petId}", petId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rex Updated\",\"speciesId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"breedId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"sex\":\"MALE\",\"birthDate\":\"2020-01-01\",\"weightKg\":16.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Rex"));
    }

    @Test
    void should_Return403Forbidden_when_CustomerUpdatesOtherPet() throws Exception {
        UUID petId = UUID.randomUUID();
        UUID speciesId = UUID.randomUUID();
        given(petService.updatePet(eq(petId), any(UpdatePetRequest.class)))
                .willThrow(new BusinessException(ErrorCode.ERR_403_FORBIDDEN));

        mockMvc.perform(put("/v1/pets/{petId}", petId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Milo Updated\",\"speciesId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"breedId\":\"3fa85f64-5717-4562-b3fc-2c963f66afa6\",\"sex\":\"MALE\",\"birthDate\":\"2020-01-01\",\"weightKg\":16.00}"))
                .andExpect(status().isForbidden());
    }



    @Test
    void listPets_success() throws Exception {
        given(petService.listPets(any(), any(), any())).willReturn(PageResponse.of(new PageImpl<>(java.util.List.of(mockPetResponse()))));

        mockMvc.perform(get("/v1/pets?ownerId=" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void listPets_noOwner_success() throws Exception {
        given(petService.listPets(any(), any())).willReturn(PageResponse.of(new PageImpl<>(java.util.List.of(mockPetResponse()))));

        mockMvc.perform(get("/v1/pets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deactivatePet_success() throws Exception {
        UUID petId = UUID.randomUUID();

        mockMvc.perform(delete("/v1/pets/{petId}", petId))
                .andExpect(status().isOk());
        verify(petService).deactivatePet(petId);
    }
}
