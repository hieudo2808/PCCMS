package com.astral.express.pccms.boarding.repository;

import com.astral.express.pccms.boarding.entity.RoomAllocation;
import com.astral.express.pccms.boarding.entity.RoomAllocationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomAllocationRepository extends JpaRepository<RoomAllocation, UUID> {

    @Query("""
            SELECT CASE WHEN COUNT(ra) > 0 THEN true ELSE false END
            FROM RoomAllocation ra
            WHERE ra.room.id = :roomId
              AND ra.statusCode = :statusCode
              AND ra.releasedAt IS NULL
              AND ra.allocatedFrom < :endAt
              AND ra.allocatedTo > :startAt
            """)
    boolean existsActiveConflict(
            @Param("roomId") UUID roomId,
            @Param("statusCode") RoomAllocationStatus statusCode,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt);

    @EntityGraph(attributePaths = {"room", "room.roomType"})
    Optional<RoomAllocation> findFirstByBookingIdAndStatusCode(UUID bookingId, RoomAllocationStatus statusCode);
}
