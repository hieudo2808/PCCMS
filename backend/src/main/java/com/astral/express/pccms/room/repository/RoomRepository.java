package com.astral.express.pccms.room.repository;

import com.astral.express.pccms.room.entity.Room;
import com.astral.express.pccms.room.entity.RoomStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    @EntityGraph(attributePaths = "roomType")
    Page<Room> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "roomType")
    Optional<Room> findWithRoomTypeById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r FROM Room r
            JOIN FETCH r.roomType rt
            WHERE r.id = :roomId
              AND r.statusCode = :statusCode
            """)
    Optional<Room> findAvailableByIdWithLock(@Param("roomId") UUID roomId, @Param("statusCode") RoomStatus statusCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r FROM Room r
            JOIN FETCH r.roomType rt
            WHERE rt.id = :roomTypeId
              AND r.statusCode = :statusCode
              AND NOT EXISTS (
                  SELECT 1 FROM RoomAllocation ra
                  WHERE ra.room = r
                    AND ra.statusCode = com.astral.express.pccms.boarding.entity.RoomAllocationStatus.ALLOCATED
                    AND ra.releasedAt IS NULL
                    AND ra.allocatedFrom < :endAt
                    AND ra.allocatedTo > :startAt
              )
            ORDER BY r.roomCode ASC
            """)
    List<Room> findAvailableByTypeWithLock(
            @Param("roomTypeId") UUID roomTypeId,
            @Param("statusCode") RoomStatus statusCode,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt);

    @Query("""
            SELECT COUNT(r) FROM Room r
            WHERE r.roomType.id = :roomTypeId
              AND r.statusCode = :statusCode
              AND NOT EXISTS (
                  SELECT 1 FROM RoomAllocation ra
                  WHERE ra.room = r
                    AND ra.statusCode = com.astral.express.pccms.boarding.entity.RoomAllocationStatus.ALLOCATED
                    AND ra.releasedAt IS NULL
                    AND ra.allocatedFrom < :endAt
                    AND ra.allocatedTo > :startAt
              )
            """)
    long countAvailableByType(
            @Param("roomTypeId") UUID roomTypeId,
            @Param("statusCode") RoomStatus statusCode,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt);

    @Query("SELECT r FROM Room r WHERE (:roomTypeId IS NULL OR r.roomType.id = :roomTypeId) AND (:statusCode IS NULL OR cast(r.statusCode as string) = :statusCode)")
    Page<Room> searchRooms(@Param("roomTypeId") UUID roomTypeId, @Param("statusCode") String statusCode, Pageable pageable);

    boolean existsByRoomCodeIgnoreCase(String roomCode);

    boolean existsByRoomCodeIgnoreCaseAndIdNot(String roomCode, UUID id);

    @Query("SELECT COUNT(ra) FROM RoomAllocation ra WHERE ra.room.id = :roomId")
    long countRoomAllocations(@Param("roomId") UUID roomId);

    Page<Room> findByRoomTypeIdAndStatusCode(UUID roomTypeId, RoomStatus statusCode, Pageable pageable);

    Page<Room> findByRoomTypeId(UUID roomTypeId, Pageable pageable);

    Page<Room> findByStatusCode(RoomStatus statusCode, Pageable pageable);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    boolean existsByRoomTypeId(UUID roomTypeId);
}
