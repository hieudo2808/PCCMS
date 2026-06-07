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

    @Column(nullable = false)
    UUID petId;

    UUID medicalRecordId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    AlertSeverity severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    String message;

    OffsetDateTime resolvedAt;

    UUID createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    OffsetDateTime createdAt;
}
