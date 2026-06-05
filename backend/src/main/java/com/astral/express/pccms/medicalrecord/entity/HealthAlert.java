package com.astral.express.pccms.medicalrecord.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "health_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HealthAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "pet_id", nullable = false)
    UUID petId;

    @Column(name = "medical_record_id")
    UUID medicalRecordId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    AlertSeverity severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    String message;

    @Column(name = "resolved_at")
    OffsetDateTime resolvedAt;

    @Column(name = "created_by")
    UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    OffsetDateTime createdAt;
}
