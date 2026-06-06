package com.astral.express.pccms.appointment.repository;

import com.astral.express.pccms.appointment.entity.GroomingStatus;
import com.astral.express.pccms.appointment.entity.GroomingTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroomingTicketRepository extends JpaRepository<GroomingTicket, UUID> {

    @Query("""
            SELECT gt FROM GroomingTicket gt
            JOIN FETCH gt.appointment a
            JOIN FETCH a.serviceOrder so
            JOIN FETCH so.pet p
            JOIN FETCH so.service svc
            LEFT JOIN FETCH gt.station
            WHERE a.scheduledStartAt >= :dayStart
              AND a.scheduledStartAt < :dayEnd
              AND gt.statusCode <> :cancelled
            ORDER BY a.scheduledStartAt ASC
            """)
    List<GroomingTicket> findBoardForDate(
            @Param("dayStart") OffsetDateTime dayStart,
            @Param("dayEnd") OffsetDateTime dayEnd,
            @Param("cancelled") GroomingStatus cancelled);

    @Query("""
            SELECT gt FROM GroomingTicket gt
            JOIN FETCH gt.appointment a
            JOIN FETCH a.serviceOrder so
            JOIN FETCH so.pet p
            JOIN FETCH so.service svc
            LEFT JOIN FETCH gt.station
            WHERE gt.id = :id
            """)
    Optional<GroomingTicket> findDetailById(@Param("id") UUID id);
}
