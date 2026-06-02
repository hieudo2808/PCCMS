package com.astral.express.pccms.medicine.mapper;

import com.astral.express.pccms.medicine.dto.request.MedicineCreateRequest;
import com.astral.express.pccms.medicine.dto.request.MedicineUpdateRequest;
import com.astral.express.pccms.medicine.dto.response.MedicineResponse;
import com.astral.express.pccms.medicine.entity.Medicine;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MedicineMapper {

    @Mapping(target = "category", ignore = true)
    Medicine toMedicine(MedicineCreateRequest request);

    @Mapping(target = "category", ignore = true)
    void updateMedicineFromRequest(MedicineUpdateRequest request, @MappingTarget Medicine medicine);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    MedicineResponse toMedicineResponse(Medicine medicine);
}
