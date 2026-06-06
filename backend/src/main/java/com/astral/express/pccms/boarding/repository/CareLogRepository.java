package com.astral.express.pccms.boarding.repository;

import com.astral.express.pccms.boarding.entity.CareLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CareLogRepository extends JpaRepository<CareLog, UUID> {

    @Query(value = """
            SELECT cl.*
            FROM care_logs cl
            INNER JOIN boarding_sessions bs ON bs.id = cl.session_id
            INNER JOIN boarding_bookings bb ON bb.id = bs.booking_id
            WHERE bb.owner_id = :ownerId
              AND bs.status_code IN ('CHECKED_IN', 'IN_STAY')
              AND (:petId IS NULL OR cl.pet_id = CAST(:petId AS uuid))
            ORDER BY cl.log_date DESC, cl.period_code DESC
            """, nativeQuery = true)
    List<CareLog> findActiveStayLogsByOwner(
            @Param("ownerId") UUID ownerId,
            @Param("petId") UUID petId);

    @Query(value = """
            SELECT DISTINCT p.id, p.name, ps.name, pb.name
            FROM pets p
            INNER JOIN boarding_bookings bb ON bb.pet_id = p.id
            INNER JOIN boarding_sessions bs ON bs.booking_id = bb.id
            LEFT JOIN pet_species ps ON ps.id = p.species_id
            LEFT JOIN pet_breeds pb ON pb.id = p.breed_id
            WHERE bb.owner_id = :ownerId
              AND bs.status_code IN ('CHECKED_IN', 'IN_STAY')
            ORDER BY p.name
            """, nativeQuery = true)
    List<Object[]> findActiveStaysByOwner(@Param("ownerId") UUID ownerId);

    @Query(value = """
            SELECT bs.id,
                   bb.pet_id,
                   p.name,
                   COALESCE(r.room_code, rt.code, bb.booking_code) AS room_label,
                   bs.actual_checkin_at,
                   bb.expected_checkin_at,
                   bb.expected_checkout_at,
                   COALESCE(string_agg(DISTINCT cl.period_code, ',' ORDER BY cl.period_code)
                            FILTER (WHERE cl.log_date = CURRENT_DATE), '') AS today_periods
            FROM boarding_sessions bs
            INNER JOIN boarding_bookings bb ON bb.id = bs.booking_id
            INNER JOIN pets p ON p.id = bb.pet_id
            LEFT JOIN room_allocations ra ON ra.id = bs.room_allocation_id
            LEFT JOIN rooms r ON r.id = ra.room_id
            LEFT JOIN room_types rt ON rt.id = bb.requested_room_type_id
            LEFT JOIN care_logs cl ON cl.session_id = bs.id
            WHERE bs.status_code IN ('CHECKED_IN', 'IN_STAY')
            GROUP BY bs.id, bb.pet_id, p.name, room_label,
                     bs.actual_checkin_at, bb.expected_checkin_at, bb.expected_checkout_at
            ORDER BY p.name
            """, nativeQuery = true)
    List<Object[]> findActiveStaysForStaff();

    List<CareLog> findBySessionIdAndLogDateOrderByPeriodCodeDesc(UUID sessionId, LocalDate logDate);

    Optional<CareLog> findBySessionIdAndLogDateAndPeriodCode(
            UUID sessionId, LocalDate logDate, String periodCode);

    @Query(value = """
            SELECT bs.id, bb.pet_id, bs.status_code::text
            FROM boarding_sessions bs
            INNER JOIN boarding_bookings bb ON bb.id = bs.booking_id
            WHERE bs.id = :sessionId
            """, nativeQuery = true)
    Optional<Object[]> findSessionContext(@Param("sessionId") UUID sessionId);
}
