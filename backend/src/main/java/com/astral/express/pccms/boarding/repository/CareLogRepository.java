package com.astral.express.pccms.boarding.repository;

import com.astral.express.pccms.boarding.entity.CareLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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
            SELECT COUNT(DISTINCT bb.pet_id)
            FROM boarding_bookings bb
            INNER JOIN boarding_sessions bs ON bs.booking_id = bb.id
            WHERE bb.owner_id = :ownerId
              AND bs.status_code IN ('CHECKED_IN', 'IN_STAY')
            """, nativeQuery = true)
    long countActiveStayPetsByOwner(@Param("ownerId") UUID ownerId);

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
}
