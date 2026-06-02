package com.astral.express.pccms.user.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID userId;

    String fullName;
    String email;
    String hashPassword;
    String avatarUrl;
    String bio;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    UserStatus statusCode = UserStatus.UNVERIFIED;
    @Builder.Default
    int failedLoginAttempts = 0;
    OffsetDateTime lockUntil;

    @Builder.Default
    boolean emailVerified = false;
    String emailVerificationToken;
    OffsetDateTime emailVerificationExpires;

    String passwordResetToken;
    OffsetDateTime passwordResetExpires;

    @CreationTimestamp
    OffsetDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roleId")
    Roles role;
}
