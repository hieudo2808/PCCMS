package com.astral.express.pccms.boarding.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "care_logs")
@Getter
public class CareLog {

    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "pet_id", nullable = false)
    private UUID petId;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(name = "period_code", nullable = false)
    private String periodCode;

    @Column(name = "feeding_status", nullable = false)
    private String feedingStatus;

    @Column(name = "hygiene_status", nullable = false)
    private String hygieneStatus;

    @Column(name = "health_note")
    private String healthNote;

    @Column(name = "staff_note")
    private String staffNote;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
