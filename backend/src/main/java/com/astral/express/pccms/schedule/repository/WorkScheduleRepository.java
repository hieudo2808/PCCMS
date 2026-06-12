package com.astral.express.pccms.schedule.repository;

import com.astral.express.pccms.schedule.entity.ScheduleStatus;
import com.astral.express.pccms.schedule.entity.WorkSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, UUID> {
    @EntityGraph(attributePaths = {"staff", "shift", "role", "examRoom", "station"})
    Page<WorkSchedule> findByWorkDateBetween(LocalDate fromDate, LocalDate toDate, Pageable pageable);

    @EntityGraph(attributePaths = {"staff", "shift", "role", "examRoom", "station"})
    List<WorkSchedule> findByWorkDateBetweenAndStatusCodeOrderByWorkDateAsc(
            LocalDate fromDate,
            LocalDate toDate,
            ScheduleStatus statusCode);

    @EntityGraph(attributePaths = {"staff", "shift", "role", "examRoom", "station"})
    Page<WorkSchedule> findByStaffIdAndWorkDateBetween(
            UUID staffId,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable);

    boolean existsByStaffIdAndWorkDateAndShiftId(UUID staffId, LocalDate workDate, UUID shiftId);

    boolean existsByStaffIdAndWorkDateAndShiftIdAndIdNot(
            UUID staffId,
            LocalDate workDate,
            UUID shiftId,
            UUID id);

    @Query(value = """
            SELECT DISTINCT ws.staff_id FROM work_schedules ws
            INNER JOIN users s ON s.id = ws.staff_id
            INNER JOIN roles r ON r.id = s.role_id
            INNER JOIN shifts sh ON sh.id = ws.shift_id
            WHERE ws.work_date = :workDate
              AND ws.status_code::text = 'ASSIGNED'
              AND r.code = 'VETERINARIAN'
              AND sh.start_time <= :slotStart
              AND sh.end_time > :slotStart
            """, nativeQuery = true)
    List<UUID> findAvailableVetIds(
            @Param("workDate") LocalDate workDate,
            @Param("slotStart") LocalTime slotStart);

    @Query(value = """
            SELECT DISTINCT ws.staff_id FROM work_schedules ws
            INNER JOIN users s ON s.id = ws.staff_id
            INNER JOIN roles r ON r.id = s.role_id
            WHERE ws.work_date = :workDate
              AND ws.status_code::text = 'ASSIGNED'
              AND r.code = 'VETERINARIAN'
            """, nativeQuery = true)
    List<UUID> findVetIdsOnDutyForDate(@Param("workDate") LocalDate workDate);
}
