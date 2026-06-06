package com.astral.express.pccms.appointment.repository;

import com.astral.express.pccms.appointment.entity.WorkSchedule;
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
