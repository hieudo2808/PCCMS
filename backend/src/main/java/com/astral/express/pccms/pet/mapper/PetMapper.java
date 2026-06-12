package com.astral.express.pccms.pet.mapper;

import com.astral.express.pccms.pet.dto.request.CreatePetRequest;
import com.astral.express.pccms.pet.dto.request.UpdatePetRequest;
import com.astral.express.pccms.pet.dto.response.PetResponse;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.medicalrecord.dto.response.HealthAlertResponse;

import java.util.HashMap;
import java.util.List;
import java.time.LocalDate;
import java.time.Period;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.AfterMapping;
import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, imports = {Map.class, HashMap.class})
public interface PetMapper {

    @Mapping(target = "speciesId", source = "pet.species.id")
    @Mapping(target = "speciesName", source = "pet.species.name")
    @Mapping(target = "breedId", source = "pet.breed.id")
    @Mapping(target = "breedName", source = "pet.breed.name")
    @Mapping(target = "ownerId", source = "pet.owner.id")
    @Mapping(target = "identificationNote", expression = "java(getStringAttribute(pet, \"identificationNote\"))")
    @Mapping(target = "specialNote", expression = "java(getStringAttribute(pet, \"specialNote\"))")
    @Mapping(target = "allergyNote", expression = "java(getStringAttribute(pet, \"allergyNote\"))")
    @Mapping(target = "nutritionNote", expression = "java(getStringAttribute(pet, \"nutritionNote\"))")
    @Mapping(target = "estimatedAgeMonths", expression = "java(calculateAge(pet.getBirthDate()))")
    PetResponse toResponse(Pets pet, List<HealthAlertResponse> healthAlerts);

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
    void updatePetFromRequest(UpdatePetRequest request, @MappingTarget Pets pet);

    @AfterMapping
    default void mapNotesToAttributes(CreatePetRequest request, @MappingTarget Pets pet) {
        if (pet.getAttributes() == null) {
            pet.setAttributes(new HashMap<>());
        }
        if (request.identificationNote() != null) pet.getAttributes().put("identificationNote", request.identificationNote());
        if (request.specialNote() != null) pet.getAttributes().put("specialNote", request.specialNote());
        if (request.allergyNote() != null) pet.getAttributes().put("allergyNote", request.allergyNote());
        if (request.nutritionNote() != null) pet.getAttributes().put("nutritionNote", request.nutritionNote());
    }

    @AfterMapping
    default void mapNotesToAttributes(UpdatePetRequest request, @MappingTarget Pets pet) {
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

    default Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) return null;
        Period period = Period.between(birthDate, LocalDate.now());
        return period.getYears() * 12 + period.getMonths();
    }
}
