package com.astral.express.pccms.medicine.repository;

import com.astral.express.pccms.medicine.entity.MedicineCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MedicineCategoryRepository extends JpaRepository<MedicineCategory, UUID> {
}
