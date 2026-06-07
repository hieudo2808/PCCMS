package com.astral.express.pccms.user.entity;

import com.astral.express.pccms.common.domain.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Users extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false, unique = true, length = 255)
    String email;

    @Column(unique = true, length = 30)
    String phone;

    @Column(name = "password_hash", nullable = false, columnDefinition = "TEXT")
    String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    String fullName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    Roles role;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status_code", nullable = false)
    @Builder.Default
    UserStatus statusCode = UserStatus.UNVERIFIED;

    @Column(name = "email_verified_at")
    OffsetDateTime emailVerifiedAt;

    @Column(name = "phone_verified_at")
    OffsetDateTime phoneVerifiedAt;

    @Column(name = "last_login_at")
    OffsetDateTime lastLoginAt;

    @Column(name = "deleted_at")
    OffsetDateTime deletedAt;
}
