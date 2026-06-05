package com.astral.express.pccms.grooming.repository;

import com.astral.express.pccms.grooming.entity.Appointment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    @EntityGraph(attributePaths = {
            "serviceOrder",
            "serviceOrder.owner",
            "serviceOrder.pet",
            "serviceOrder.service",
            "assignedStaff"
    })
    Optional<Appointment> findWithDetailsById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Appointment> findWithLockById(UUID id);
}
