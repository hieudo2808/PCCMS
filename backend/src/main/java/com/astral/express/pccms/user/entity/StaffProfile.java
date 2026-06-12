package com.astral.express.pccms.user.entity;

import com.astral.express.pccms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "staff_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffProfile extends AuditableEntity {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private Users user;

    @Column(name = "staff_code", unique = true, length = 60)
    private String staffCode;

    @Column(name = "professional_title", length = 120)
    private String professionalTitle;

    @Column(name = "license_number", length = 120)
    private String licenseNumber;

    @Column(name = "specialization", length = 200)
    private String specialization;

    @Column(name = "is_service_provider", nullable = false)
    @Builder.Default
    private Boolean isServiceProvider = true;
}
