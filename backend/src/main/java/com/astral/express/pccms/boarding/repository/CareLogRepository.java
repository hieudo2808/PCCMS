package com.astral.express.pccms.boarding.repository;

import com.astral.express.pccms.boarding.entity.CareLog;
import com.astral.express.pccms.boarding.entity.CarePeriod;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface CareLogRepository extends JpaRepository<CareLog, UUID> {
    boolean existsBySessionIdAndLogDateAndPeriodCode(UUID sessionId, LocalDate logDate, CarePeriod periodCode);

    @EntityGraph(attributePaths = {"staff", "session", "pet"})
    List<CareLog> findBySessionBookingIdOrderByLogDateDescCreatedAtDesc(UUID bookingId);
}
