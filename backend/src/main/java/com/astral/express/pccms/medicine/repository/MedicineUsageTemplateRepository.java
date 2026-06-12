package com.astral.express.pccms.medicine.repository;

import com.astral.express.pccms.medicine.entity.MedicineUsageTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MedicineUsageTemplateRepository extends JpaRepository<MedicineUsageTemplate, UUID> {
    List<MedicineUsageTemplate> findByMedicineIdAndIsActiveTrueOrderBySortOrderAsc(UUID medicineId);
    
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM MedicineUsageTemplate t WHERE LOWER(t.label) = LOWER(:label) AND t.medicine.id = :medicineId AND t.isActive = true")
    boolean existsByLabelAndMedicineId(String label, UUID medicineId);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM MedicineUsageTemplate t WHERE LOWER(t.label) = LOWER(:label) AND t.medicine.id = :medicineId AND t.id <> :excludeId AND t.isActive = true")
    boolean existsByLabelAndMedicineIdAndIdNot(String label, UUID medicineId, UUID excludeId);
}
