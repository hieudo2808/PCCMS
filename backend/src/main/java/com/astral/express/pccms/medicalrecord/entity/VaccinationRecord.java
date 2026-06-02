package com.astral.express.pccms.medicalrecord.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vaccination_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VaccinationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false)
    UUID petId;

    UUID medicalRecordId;

    @Column(nullable = false, length = 160)
    String vaccineName;

    @Column(nullable = false)
    LocalDate vaccinationDate;

    LocalDate nextDueDate;

    String note;

    UUID createdBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    OffsetDateTime createdAt;
}
