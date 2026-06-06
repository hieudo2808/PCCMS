package com.astral.express.pccms.appointment.repository;

import com.astral.express.pccms.appointment.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.serviceOrder so
            JOIN FETCH so.service
            JOIN FETCH so.owner o
            JOIN FETCH so.pet p
            LEFT JOIN FETCH a.assignedStaff
            LEFT JOIN FETCH a.examRoom
            WHERE so.owner.id = :ownerId
            ORDER BY a.scheduledStartAt DESC
            """)
    Page<Appointment> findByOwnerId(@Param("ownerId") UUID ownerId, Pageable pageable);

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.serviceOrder so
            JOIN FETCH so.service
            JOIN FETCH so.owner o
            JOIN FETCH so.pet p
            LEFT JOIN FETCH a.assignedStaff
            WHERE a.id = :id
            """)
    Optional<Appointment> findDetailById(@Param("id") UUID id);

    @Query(value = """
            SELECT COUNT(*) FROM appointments a
            WHERE a.exam_room_id = :roomId
              AND a.status_code NOT IN ('CANCELLED', 'COMPLETED')
              AND a.scheduled_start_at < :endAt
              AND a.scheduled_end_at > :startAt
            """, nativeQuery = true)
    long countOverlappingInRoom(
            @Param("roomId") UUID roomId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt);

    @Query(value = """
            SELECT COUNT(*) FROM appointments a
            WHERE a.assigned_staff_id = :staffId
              AND a.status_code NOT IN ('CANCELLED', 'COMPLETED')
              AND a.scheduled_start_at < :endAt
              AND a.scheduled_end_at > :startAt
            """, nativeQuery = true)
    long countOverlappingForStaff(
            @Param("staffId") UUID staffId,
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt);

    @Query("""
            SELECT a FROM Appointment a
            JOIN FETCH a.serviceOrder so
            JOIN FETCH so.service
            JOIN FETCH so.owner o
            JOIN FETCH so.pet p
            LEFT JOIN FETCH a.assignedStaff
            WHERE a.scheduledStartAt >= :dayStart
              AND a.scheduledStartAt < :dayEnd
            ORDER BY a.scheduledStartAt ASC
            """)
    List<Appointment> findAppointmentsForDay(
            @Param("dayStart") OffsetDateTime dayStart,
            @Param("dayEnd") OffsetDateTime dayEnd);

    @Query(value = """
            SELECT COUNT(*) FROM appointments a
            WHERE a.appointment_type = 'GROOMING'
              AND a.status_code NOT IN ('CANCELLED', 'COMPLETED')
              AND a.scheduled_start_at < :endAt
              AND a.scheduled_end_at > :startAt
            """, nativeQuery = true)
    long countOverlappingGrooming(
            @Param("startAt") OffsetDateTime startAt,
            @Param("endAt") OffsetDateTime endAt);

}
