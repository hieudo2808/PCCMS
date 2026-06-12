package com.astral.express.pccms.boarding.repository;

import com.astral.express.pccms.boarding.entity.BoardingSession;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardingSessionRepository extends JpaRepository<BoardingSession, UUID> {

    @EntityGraph(attributePaths = {"booking", "booking.owner", "booking.pet", "booking.requestedRoomType", "roomAllocation", "roomAllocation.room"})
    Optional<BoardingSession> findByBookingId(UUID bookingId);

    @EntityGraph(attributePaths = {"booking", "booking.owner", "booking.pet", "booking.requestedRoomType", "roomAllocation", "roomAllocation.room"})
    Optional<BoardingSession> findWithDetailsById(UUID id);
}
