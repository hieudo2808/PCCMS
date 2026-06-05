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

    @Column(name = "pet_id", nullable = false)
    UUID petId;

    @Column(name = "medical_record_id")
    UUID medicalRecordId;

    @Column(name = "vaccine_name", nullable = false, length = 160)
    String vaccineName;

    @Column(name = "vaccination_date", nullable = false)
    LocalDate vaccinationDate;

    @Column(name = "next_due_date")
    LocalDate nextDueDate;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @Column(name = "created_by")
    UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    OffsetDateTime createdAt;
}
