package com.astral.express.pccms.medicalrecord.mapper;

import com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordResponse;
import com.astral.express.pccms.medicalrecord.entity.MedicalRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MedicalRecordMapper {
    @Mapping(target = "petName", ignore = true)
    @Mapping(target = "vetName", ignore = true)
    MedicalRecordResponse toResponse(MedicalRecord entity);
}
