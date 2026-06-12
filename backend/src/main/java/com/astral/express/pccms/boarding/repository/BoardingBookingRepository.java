package com.astral.express.pccms.boarding.repository;

import com.astral.express.pccms.boarding.entity.BoardingBooking;
import com.astral.express.pccms.boarding.entity.BoardingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardingBookingRepository extends JpaRepository<BoardingBooking, UUID> {

    @EntityGraph(attributePaths = {"owner", "pet", "requestedRoomType", "serviceOrder"})
    Page<BoardingBooking> findByOwnerIdOrderByExpectedCheckinAtDesc(UUID ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "pet", "requestedRoomType", "serviceOrder"})
    Page<BoardingBooking> findByStatusCodeOrderByExpectedCheckinAtAsc(BoardingStatus statusCode, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "pet", "requestedRoomType", "serviceOrder"})
    Page<BoardingBooking> findAllByOrderByExpectedCheckinAtAsc(Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "pet", "requestedRoomType", "serviceOrder"})
    Optional<BoardingBooking> findWithDetailsById(UUID id);

    @Query("""
            SELECT CASE WHEN COUNT(booking) > 0 THEN true ELSE false END
            FROM BoardingBooking booking
            WHERE booking.owner.id = :ownerId
              AND booking.pet.id = :petId
              AND booking.statusCode IN :statuses
              AND booking.expectedCheckinAt < :endAt
              AND booking.expectedCheckoutAt > :startAt
            """)
    boolean existsOwnerPetBookingConflict(
            @Param("ownerId") UUID ownerId,
            @Param("petId") UUID petId,
            @Param("statuses") Collection<BoardingStatus> statuses,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt);

    @Query("""
            SELECT bb FROM BoardingBooking bb
            JOIN FETCH bb.pet p
            JOIN FETCH bb.requestedRoomType rt
            WHERE bb.owner.id = :ownerId
            ORDER BY bb.expectedCheckinAt DESC
            """)
    List<BoardingBooking> findByOwnerId(@Param("ownerId") UUID ownerId);

    @Query(value = """
            SELECT COALESCE(MAX(CAST(SUBSTRING(booking_code FROM 3) AS INTEGER)), 0)
            FROM boarding_bookings
            WHERE booking_code ~ '^BR[0-9]+$'
            """, nativeQuery = true)
    long maxBookingSequence();
}
