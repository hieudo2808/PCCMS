package com.astral.express.pccms.room.entity;

import com.astral.express.pccms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "room_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomType extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 60)
    private String code;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "default_capacity", nullable = false)
    @Builder.Default
    private Integer defaultCapacity = 1;

    @Column(name = "base_daily_price_vnd", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal baseDailyPriceVnd = BigDecimal.ZERO;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
