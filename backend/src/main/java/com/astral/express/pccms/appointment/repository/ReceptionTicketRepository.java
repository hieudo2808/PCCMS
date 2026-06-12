package com.astral.express.pccms.appointment.repository;

import com.astral.express.pccms.appointment.entity.AppointmentStatus;
import com.astral.express.pccms.appointment.entity.ReceptionTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface ReceptionTicketRepository extends JpaRepository<ReceptionTicket, UUID> {

    Optional<ReceptionTicket> findByAppointmentId(UUID appointmentId);

    @Query("""
            SELECT COALESCE(MAX(rt.queueNumber), 0) FROM ReceptionTicket rt
            WHERE rt.assignedVet.id = :vetId
              AND rt.checkedInAt >= :dayStart
              AND rt.checkedInAt < :dayEnd
            """)
    int findMaxQueueNumberForVet(
            @Param("vetId") UUID vetId,
            @Param("dayStart") OffsetDateTime dayStart,
            @Param("dayEnd") OffsetDateTime dayEnd);

    @Query("""
            SELECT rt FROM ReceptionTicket rt
            JOIN FETCH rt.appointment a
            JOIN FETCH a.serviceOrder so
            JOIN FETCH so.owner o
            JOIN FETCH so.pet p
            WHERE a.statusCode = :status
              AND rt.assignedVet.id = :vetId
              AND rt.checkedInAt >= :dayStart
              AND rt.checkedInAt < :dayEnd
            ORDER BY rt.queueNumber ASC NULLS LAST, rt.checkedInAt ASC
            """)
    List<ReceptionTicket> findVetQueueTickets(
            @Param("vetId") UUID vetId,
            @Param("dayStart") OffsetDateTime dayStart,
            @Param("dayEnd") OffsetDateTime dayEnd,
            @Param("status") AppointmentStatus status);
}
