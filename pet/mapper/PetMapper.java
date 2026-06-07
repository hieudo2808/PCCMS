package com.astral.express.pccms.pet.mapper;

import com.astral.express.pccms.pet.dto.request.CreatePetRequest;
import com.astral.express.pccms.pet.dto.response.PetResponse;
import com.astral.express.pccms.pet.entity.Pets;
import java.util.HashMap;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, imports = {java.util.Map.class, java.util.HashMap.class})
public interface PetMapper {

    @Mapping(target = "speciesId", source = "pet.species.id")
    @Mapping(target = "breedId", source = "pet.breed.id")
    @Mapping(target = "ownerId", source = "pet.owner.id")
    @Mapping(target = "identificationNote", expression = "java(getStringAttribute(pet, \"identificationNote\"))")
    @Mapping(target = "specialNote", expression = "java(getStringAttribute(pet, \"specialNote\"))")
    @Mapping(target = "allergyNote", expression = "java(getStringAttribute(pet, \"allergyNote\"))")
    @Mapping(target = "nutritionNote", expression = "java(getStringAttribute(pet, \"nutritionNote\"))")
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

    @org.mapstruct.AfterMapping
    default void mapNotesToAttributes(CreatePetRequest request, @org.mapstruct.MappingTarget Pets pet) {
        if (pet.getAttributes() == null) {
            pet.setAttributes(new HashMap<>());
        }
        if (request.identificationNote() != null) pet.getAttributes().put("identificationNote", request.identificationNote());
        if (request.specialNote() != null) pet.getAttributes().put("specialNote", request.specialNote());
        if (request.allergyNote() != null) pet.getAttributes().put("allergyNote", request.allergyNote());
        if (request.nutritionNote() != null) pet.getAttributes().put("nutritionNote", request.nutritionNote());
    }

    @org.mapstruct.AfterMapping
    default void mapNotesToAttributes(com.astral.express.pccms.pet.dto.request.UpdatePetRequest request, @org.mapstruct.MappingTarget Pets pet) {
        if (pet.getAttributes() == null) {
            pet.setAttributes(new HashMap<>());
        }
        if (request.identificationNote() != null) pet.getAttributes().put("identificationNote", request.identificationNote());
        if (request.specialNote() != null) pet.getAttributes().put("specialNote", request.specialNote());
        if (request.allergyNote() != null) pet.getAttributes().put("allergyNote", request.allergyNote());
        if (request.nutritionNote() != null) pet.getAttributes().put("nutritionNote", request.nutritionNote());
    }

    default String getStringAttribute(Pets pet, String key) {
        if (pet.getAttributes() != null && pet.getAttributes().containsKey(key)) {
            Object val = pet.getAttributes().get(key);
            return val != null ? val.toString() : null;
        }
        return null;
    }
}
