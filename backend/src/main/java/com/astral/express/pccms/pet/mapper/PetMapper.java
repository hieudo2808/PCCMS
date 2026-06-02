package com.astral.express.pccms.pet.mapper;

import com.astral.express.pccms.pet.dto.request.CreatePetRequest;
import com.astral.express.pccms.pet.dto.response.PetResponse;
import com.astral.express.pccms.pet.entity.Pets;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PetMapper {

    @Mapping(target = "speciesId", source = "pet.species.id")
    @Mapping(target = "breedId", source = "pet.breed.id")
    @Mapping(target = "ownerId", source = "pet.owner.userId")
    PetResponse toResponse(Pets pet, java.util.List<com.astral.express.pccms.medicalrecord.dto.response.HealthAlertResponse> healthAlerts);

    @Mapping(target = "species", ignore = true)
    @Mapping(target = "breed", ignore = true)
    @Mapping(target = "owner", ignore = true)
    Pets toEntity(CreatePetRequest request);

    @Mapping(target = "species", ignore = true)
    @Mapping(target = "breed", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "id", ignore = true)
    void updatePetFromRequest(com.astral.express.pccms.pet.dto.request.UpdatePetRequest request, @org.mapstruct.MappingTarget Pets pet);
}
