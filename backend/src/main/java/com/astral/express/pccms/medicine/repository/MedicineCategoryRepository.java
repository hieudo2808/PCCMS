package com.astral.express.pccms.medicine.repository;

import com.astral.express.pccms.medicine.entity.MedicineCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MedicineCategoryRepository extends JpaRepository<MedicineCategory, UUID> {
    List<MedicineCategory> findByIsActiveTrueOrderByNameAsc();

    List<MedicineCategory> findAllByOrderByNameAsc();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);
}
