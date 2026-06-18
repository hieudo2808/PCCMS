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
    boolean existsBySessionIdAndLogDateAndPeriodCodeAndDeletedAtIsNull(UUID sessionId, LocalDate logDate, CarePeriod periodCode);

    @EntityGraph(attributePaths = {"staff", "session", "pet"})
    List<CareLog> findBySessionBookingIdAndDeletedAtIsNullOrderByLogDateDescCreatedAtDesc(UUID bookingId);

    @EntityGraph(attributePaths = {"staff", "session", "pet"})
    Optional<CareLog> findBySessionIdAndLogDateAndPeriodCodeAndDeletedAtIsNull(UUID sessionId, LocalDate logDate, CarePeriod periodCode);

    @EntityGraph(attributePaths = {"staff", "session", "pet"})
    List<CareLog> findBySessionIdAndLogDateAndDeletedAtIsNullOrderByPeriodCodeDesc(UUID sessionId, LocalDate logDate);

    @Query(value = """
        SELECT s.id as sessionId, s.pet_id as petId, p.name as petName, r.room_code as roomLabel,
               s.actual_checkin_at as checkinDate, s.expected_checkin_at as expCheckin, s.expected_checkout_at as expCheckout,
               (SELECT string_agg(cl.period_code, ',') FROM care_logs cl WHERE cl.session_id = s.id AND cl.log_date = CURRENT_DATE AND cl.deleted_at IS NULL) as todayPeriods
        FROM boarding_sessions s
        JOIN pets p ON s.pet_id = p.id
        LEFT JOIN rooms r ON s.room_id = r.id
        WHERE s.status_code IN ('CHECKED_IN', 'IN_STAY')
        """, nativeQuery = true)
    List<StaffActiveStayRow> findActiveStaysForStaff();

    @Query(value = """
        SELECT s.id as sessionId, s.pet_id as petId, s.status_code as status
        FROM boarding_sessions s
        WHERE s.id = :sessionId
        """, nativeQuery = true)
    Optional<SessionContextRow> findSessionContext(@Param("sessionId") UUID sessionId);

    @Query(value = """
        SELECT p.id as petId, p.name as petName, ps.name as speciesName, pb.name as breedName
        FROM boarding_sessions s
        JOIN pets p ON s.pet_id = p.id
        LEFT JOIN pet_species ps ON p.species_id = ps.id
        LEFT JOIN pet_breeds pb ON p.breed_id = pb.id
        WHERE s.status_code IN ('CHECKED_IN', 'IN_STAY') AND p.owner_id = :ownerId
        """, nativeQuery = true)
    List<OwnerActiveStayRow> findActiveStaysByOwner(@Param("ownerId") UUID ownerId);

    @Query("""
        SELECT cl FROM CareLog cl
        JOIN cl.session s
        JOIN cl.pet p
        WHERE p.owner.id = :ownerId AND p.id = :petId AND s.statusCode IN ('CHECKED_IN', 'IN_STAY') AND cl.deletedAt IS NULL
        ORDER BY cl.logDate DESC, cl.periodCode DESC
        """)
    @EntityGraph(attributePaths = {"staff", "session", "pet"})
    List<CareLog> findActiveStayLogsByOwner(@Param("ownerId") UUID ownerId, @Param("petId") UUID petId);

    interface StaffActiveStayRow {
        UUID getSessionId();
        UUID getPetId();
        String getPetName();
        String getRoomLabel();
        Object getCheckinDate();
        Object getExpCheckin();
        Object getExpCheckout();
        String getTodayPeriods();
    }

    interface SessionContextRow {
        UUID getSessionId();
        UUID getPetId();
        String getStatus();
    }

    interface OwnerActiveStayRow {
        UUID getPetId();
        String getPetName();
        String getSpeciesName();
        String getBreedName();
    }
}
