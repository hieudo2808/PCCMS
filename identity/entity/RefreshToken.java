package com.astral.express.pccms.identity.entity;


import com.astral.express.pccms.user.entity.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

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
    private UUID id;

    @Column(name = "token_hash")
    private String hashedToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @CreationTimestamp
    @Column(name = "issued_at", updatable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;
    
    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;
}
