package com.astral.express.pccms.medicalrecord.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "prescription_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(name = "medicine_id", nullable = false)
    private UUID medicineId;

    @Column(name = "dosage", nullable = false, length = 120)
    private String dosage;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "instruction", columnDefinition = "TEXT")
    private String instruction;

    @Column(name = "unit_price_vnd", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPriceVnd;
}
