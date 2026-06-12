package com.astral.express.pccms.boarding.repository;

import com.astral.express.pccms.boarding.entity.CareLog;
import com.astral.express.pccms.boarding.entity.CarePeriod;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

@Repository
public interface CareLogRepository extends JpaRepository<CareLog, UUID> {
    boolean existsBySessionIdAndLogDateAndPeriodCode(UUID sessionId, LocalDate logDate, CarePeriod periodCode);

    @EntityGraph(attributePaths = {"staff", "session", "pet"})
    List<CareLog> findBySessionBookingIdOrderByLogDateDescCreatedAtDesc(UUID bookingId);

    @EntityGraph(attributePaths = {"staff", "session", "pet"})
    Optional<CareLog> findBySessionIdAndLogDateAndPeriodCode(UUID sessionId, LocalDate logDate, CarePeriod periodCode);

    @EntityGraph(attributePaths = {"staff", "session", "pet"})
    List<CareLog> findBySessionIdAndLogDateOrderByPeriodCodeDesc(UUID sessionId, LocalDate logDate);

    @Query(value = """
        SELECT s.id as sessionId, s.pet_id as petId, p.name as petName, r.room_code as roomLabel,
               s.actual_checkin_at as checkinDate, s.expected_checkin_at as expCheckin, s.expected_checkout_at as expCheckout,
               (SELECT string_agg(cl.period_code, ',') FROM care_logs cl WHERE cl.session_id = s.id AND cl.log_date = CURRENT_DATE) as todayPeriods
        FROM boarding_sessions s
        JOIN pets p ON s.pet_id = p.id
        LEFT JOIN rooms r ON s.room_id = r.id
        WHERE s.status_code IN ('CHECKED_IN', 'IN_STAY')
        """, nativeQuery = true)
    List<Object[]> findActiveStaysForStaff();

    @Query(value = """
        SELECT s.id as sessionId, s.pet_id as petId, s.status_code as status
        FROM boarding_sessions s
        WHERE s.id = :sessionId
        """, nativeQuery = true)
    Optional<Object[]> findSessionContext(@Param("sessionId") UUID sessionId);

    @Query(value = """
        SELECT s.id as sessionId, p.name as petName, r.room_code as roomLabel,
               (SELECT string_agg(cl.period_code, ',') FROM care_logs cl WHERE cl.session_id = s.id AND cl.log_date = CURRENT_DATE) as todayLogSummary
        FROM boarding_sessions s
        JOIN pets p ON s.pet_id = p.id
        LEFT JOIN rooms r ON s.room_id = r.id
        WHERE s.status_code IN ('CHECKED_IN', 'IN_STAY') AND p.owner_id = :ownerId
        """, nativeQuery = true)
    List<Object[]> findActiveStaysByOwner(@Param("ownerId") UUID ownerId);

    @Query("""
        SELECT cl FROM CareLog cl
        JOIN cl.session s
        JOIN cl.pet p
        WHERE p.owner.id = :ownerId AND p.id = :petId AND s.statusCode IN ('CHECKED_IN', 'IN_STAY')
        ORDER BY cl.logDate DESC, cl.periodCode DESC
        """)
    @EntityGraph(attributePaths = {"staff", "session", "pet"})
    List<CareLog> findActiveStayLogsByOwner(@Param("ownerId") UUID ownerId, @Param("petId") UUID petId);
}
