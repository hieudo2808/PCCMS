package com.astral.express.pccms.grooming.repository;

import com.astral.express.pccms.grooming.entity.GroomingStatus;
import com.astral.express.pccms.grooming.entity.GroomingTicket;
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
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroomingTicketRepository extends JpaRepository<GroomingTicket, UUID> {

    @EntityGraph(attributePaths = {
            "appointment",
            "appointment.serviceOrder",
            "appointment.serviceOrder.owner",
            "appointment.serviceOrder.pet",
            "appointment.serviceOrder.service",
            "appointment.assignedStaff",
            "assignedStaff",
            "station"
    })
    Optional<GroomingTicket> findWithDetailsById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
            "appointment",
            "appointment.serviceOrder",
            "appointment.serviceOrder.owner",
            "appointment.serviceOrder.pet",
            "appointment.serviceOrder.service",
            "appointment.assignedStaff",
            "assignedStaff",
            "station"
    })
    Optional<GroomingTicket> findLockedWithDetailsById(UUID id);

    @EntityGraph(attributePaths = {
            "appointment",
            "appointment.serviceOrder",
            "appointment.serviceOrder.owner",
            "appointment.serviceOrder.pet",
            "appointment.serviceOrder.service",
            "appointment.assignedStaff",
            "assignedStaff",
            "station"
    })
    Page<GroomingTicket> findByAppointmentServiceOrderOwnerIdOrderByAppointmentScheduledStartAtDesc(UUID ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {
            "appointment",
            "appointment.serviceOrder",
            "appointment.serviceOrder.owner",
            "appointment.serviceOrder.pet",
            "appointment.serviceOrder.service",
            "appointment.assignedStaff",
            "assignedStaff",
            "station"
    })
    Page<GroomingTicket> findByStatusCodeOrderByAppointmentScheduledStartAtAsc(GroomingStatus statusCode, Pageable pageable);

    @EntityGraph(attributePaths = {
            "appointment",
            "appointment.serviceOrder",
            "appointment.serviceOrder.owner",
            "appointment.serviceOrder.pet",
            "appointment.serviceOrder.service",
            "appointment.assignedStaff",
            "assignedStaff",
            "station"
    })
    Page<GroomingTicket> findAllByOrderByAppointmentScheduledStartAtAsc(Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(ticket) > 0 THEN true ELSE false END
            FROM GroomingTicket ticket
            JOIN ticket.appointment appointment
            WHERE ticket.station.id = :stationId
              AND ticket.statusCode IN :statuses
              AND appointment.scheduledStartAt < :endAt
              AND appointment.scheduledEndAt > :startAt
              AND (:excludedTicketId IS NULL OR ticket.id <> :excludedTicketId)
            """)
    boolean existsStationConflict(
            @Param("stationId") UUID stationId,
            @Param("statuses") Collection<GroomingStatus> statuses,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt,
            @Param("excludedTicketId") UUID excludedTicketId);

    @Query("""
            SELECT CASE WHEN COUNT(ticket) > 0 THEN true ELSE false END
            FROM GroomingTicket ticket
            JOIN ticket.appointment appointment
            JOIN appointment.serviceOrder serviceOrder
            WHERE serviceOrder.owner.id = :ownerId
              AND serviceOrder.pet.id = :petId
              AND serviceOrder.service.id = :serviceId
              AND ticket.statusCode IN :statuses
              AND appointment.scheduledStartAt < :endAt
              AND appointment.scheduledEndAt > :startAt
            """)
    boolean existsOwnerBookingConflict(
            @Param("ownerId") UUID ownerId,
            @Param("petId") UUID petId,
            @Param("serviceId") UUID serviceId,
            @Param("statuses") Collection<GroomingStatus> statuses,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt);
}
