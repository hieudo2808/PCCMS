package com.astral.express.pccms.billing.entity;

import com.astral.express.pccms.common.domain.AuditableEntity;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.user.entity.Users;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "invoice_code", nullable = false, unique = true, length = 60)
    private String invoiceCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Users owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id")
    private Pets pet;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status_code", nullable = false)
    @Builder.Default
    private InvoiceStatus statusCode = InvoiceStatus.UNPAID;

    @Column(name = "issued_at", nullable = false)
    @Builder.Default
    private OffsetDateTime issuedAt = OffsetDateTime.now();

    @Column(name = "due_at")
    private OffsetDateTime dueAt;

    @Column(name = "discount_vnd", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal discountVnd = BigDecimal.ZERO;

    @Column(name = "tax_vnd", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal taxVnd = BigDecimal.ZERO;

    @Column(name = "total_amount_vnd", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalAmountVnd = BigDecimal.ZERO;

    @Column(name = "paid_amount_vnd", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal paidAmountVnd = BigDecimal.ZERO;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Users createdBy;
}
