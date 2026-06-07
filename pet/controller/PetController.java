package com.astral.express.pccms.pet.controller;

import com.astral.express.pccms.pet.dto.request.CreatePetRequest;
import com.astral.express.pccms.pet.dto.request.UpdatePetRequest;
import com.astral.express.pccms.pet.dto.response.PetResponse;
import com.astral.express.pccms.pet.service.PetService;
import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @GetMapping
    @PreAuthorize("hasRole('OWNER') or hasRole('CUSTOMER') or hasRole('STAFF') or hasRole('RECEPTIONIST') or hasRole('VETERINARIAN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<PetResponse>>> listPets(
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(petService.listPets(isActive, pageable)));
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PetResponse>> createPet(@Valid @RequestBody CreatePetRequest request) {
        PetResponse response = petService.createPet(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @PutMapping("/{petId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('RECEPTIONIST') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PetResponse>> updatePet(
            @PathVariable UUID petId,
            @Valid @RequestBody UpdatePetRequest request) {
        PetResponse response = petService.updatePet(petId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Thao tác thành công"));
    }

    @GetMapping("/{petId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('RECEPTIONIST') or hasRole('VETERINARIAN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PetResponse>> getPet(@PathVariable UUID petId) {
        PetResponse response = petService.getPet(petId);
        return ResponseEntity.ok(ApiResponse.success(response, "Thao tác thành công"));
    }
}
