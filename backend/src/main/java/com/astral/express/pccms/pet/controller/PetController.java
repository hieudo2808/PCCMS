package com.astral.express.pccms.pet.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.pet.dto.request.CreatePetRequest;
import com.astral.express.pccms.pet.dto.request.UpdatePetRequest;
import com.astral.express.pccms.pet.dto.response.PetResponse;
import com.astral.express.pccms.pet.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;



    @PostMapping
    @PreAuthorize("hasAuthority('PET_CREATE')")
    public ResponseEntity<ApiResponse<PetResponse>> createPet(@Valid @RequestBody CreatePetRequest request) {
        PetResponse response = petService.createPet(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PET_READ')")
    public ResponseEntity<ApiResponse<PageResponse<PetResponse>>> listPets(
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<PetResponse> response = ownerId == null
                ? petService.listPets(isActive, pageable)
                : petService.listPets(ownerId, isActive, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{petId}")
    @PreAuthorize("hasAuthority('PET_READ')")
    public ResponseEntity<ApiResponse<PetResponse>> getPet(@PathVariable UUID petId) {
        PetResponse response = petService.getPet(petId);
        return ResponseEntity.ok(ApiResponse.success(response, "Thao tác thành công"));
    }

    @PutMapping("/{petId}")
    @PreAuthorize("hasAuthority('PET_UPDATE')")
    public ResponseEntity<ApiResponse<PetResponse>> updatePet(
            @PathVariable UUID petId,
            @Valid @RequestBody UpdatePetRequest request) {
        PetResponse response = petService.updatePet(petId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Thao tác thành công"));
    }

    @DeleteMapping("/{petId}")
    @PreAuthorize("hasAuthority('PET_DELETE')")
    public ResponseEntity<ApiResponse<Void>> deactivatePet(@PathVariable UUID petId) {
        petService.deactivatePet(petId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã ngừng hoạt động hồ sơ thú cưng"));
    }
}
