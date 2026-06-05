package com.astral.express.pccms.identity.entity;

import com.astral.express.pccms.user.entity.Users;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(name = "token_hash", nullable = false, unique = true, columnDefinition = "TEXT")
    String hashedToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    Users user;

    @CreationTimestamp
    @Column(name = "issued_at", nullable = false, updatable = false)
    OffsetDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    OffsetDateTime revokedAt;

    @Column(name = "revoked_reason", columnDefinition = "TEXT")
    String revokedReason;

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void setRevoked(boolean revoked) {
        if (revoked) {
            if (revokedAt == null) {
                revokedAt = OffsetDateTime.now();
            }
        } else {
            revokedAt = null;
        }
    }

    public static class RefreshTokenBuilder {
        public RefreshTokenBuilder isRevoked(boolean isRevoked) {
            this.revokedAt = isRevoked ? OffsetDateTime.now() : null;
            return this;
        }
    }
}
