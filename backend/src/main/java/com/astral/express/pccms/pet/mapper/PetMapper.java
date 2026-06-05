package com.astral.express.pccms.pet.mapper;

import com.astral.express.pccms.pet.dto.request.CreatePetRequest;
import com.astral.express.pccms.pet.dto.response.PetResponse;
import com.astral.express.pccms.pet.entity.Pets;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PetMapper {

    @Mapping(target = "speciesId", source = "species.id")
    @Mapping(target = "speciesName", source = "species.name")
    @Mapping(target = "breedId", source = "breed.id")
    @Mapping(target = "breedName", source = "breed.name")
    @Mapping(target = "ownerId", source = "owner.id")
    PetResponse toResponse(Pets pet);

    @Mapping(target = "species", ignore = true)
    @Mapping(target = "breed", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    Pets toEntity(CreatePetRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "species", ignore = true)
    @Mapping(target = "breed", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    void updatePetFromRequest(com.astral.express.pccms.pet.dto.request.UpdatePetRequest request, @MappingTarget Pets pet);
}
