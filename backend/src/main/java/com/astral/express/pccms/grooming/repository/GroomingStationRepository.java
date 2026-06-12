package com.astral.express.pccms.grooming.repository;

import com.astral.express.pccms.grooming.entity.GroomingStation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroomingStationRepository extends JpaRepository<GroomingStation, UUID> {

    List<GroomingStation> findByIsActiveTrueOrderByStationCodeAsc();

    Optional<GroomingStation> findByIdAndIsActiveTrue(UUID id);

    boolean existsByStationCode(String stationCode);

    boolean existsByStationCodeAndIdNot(String stationCode, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GroomingStation> findWithLockById(UUID id);
}
