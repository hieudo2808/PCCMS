package com.astral.express.pccms.medicine.repository;

import com.astral.express.pccms.medicine.entity.Medicine;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Medicine m WHERE m.id = :id AND m.isActive = true")
    Optional<Medicine> findByIdWithLock(UUID id);

    boolean existsByMedicineCode(String medicineCode);

    boolean existsByMedicineCodeAndIdNot(String medicineCode, UUID id);

    boolean existsByNameAndUnit(String name, String unit);

    boolean existsByNameAndUnitAndIdNot(String name, String unit, UUID id);

    Page<Medicine> findByCategoryId(UUID categoryId, Pageable pageable);

    boolean existsByCategoryId(UUID categoryId);
}
