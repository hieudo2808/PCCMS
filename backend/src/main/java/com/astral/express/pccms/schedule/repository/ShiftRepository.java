package com.astral.express.pccms.schedule.repository;

import com.astral.express.pccms.schedule.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, UUID> {
    List<Shift> findByIsActiveTrueOrderByStartTimeAsc();
}
