package com.astral.express.pccms.identity.repository;

import com.astral.express.pccms.identity.entity.RefreshToken;
import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.UserStatus;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.RoleRepository;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@Transactional
class RefreshTokenRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private Users savedUser;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Roles role = Roles.builder()
                .code("TEST_ROLE")
                .name("Test Role")
                .description("A role for testing")
                .isActive(true)
                .build();
        role = roleRepository.saveAndFlush(role);

        Users user = Users.builder()
                .email("test@pccms.vn")
                .passwordHash("hashed_password")
                .fullName("Test User")
                .role(role)
                .statusCode(UserStatus.ACTIVE)
                .build();
        savedUser = userRepository.saveAndFlush(user);
    }

    @Test
    void should_FindToken_when_Exists() {
        // Arrange
        RefreshToken token = RefreshToken.builder()
                .tokenHash("hashed_token")
                .user(savedUser)
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .revokedAt(null)
                .build();
        refreshTokenRepository.saveAndFlush(token);

        // Act
        Optional<RefreshToken> foundToken = refreshTokenRepository.findByTokenHash("hashed_token");

        // Assert
        assertThat(foundToken).isPresent();
        assertThat(foundToken.get().getUser().getId()).isEqualTo(savedUser.getId());
    }

    @Test
    void should_RevokeAllUserTokens_when_UpdateQueryExecuted() {
        // Arrange
        RefreshToken token1 = RefreshToken.builder()
                .tokenHash("hash1")
                .user(savedUser)
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .revokedAt(null)
                .build();
        RefreshToken token2 = RefreshToken.builder()
                .tokenHash("hash2")
                .user(savedUser)
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .revokedAt(null)
                .build();
        refreshTokenRepository.save(token1);
        refreshTokenRepository.save(token2);
        refreshTokenRepository.flush();

        // Act
        int updated = refreshTokenRepository.revokeAllUserTokens(savedUser.getId());

        // Assert
        assertThat(updated).isEqualTo(2);
        
        Optional<RefreshToken> retrieved1 = refreshTokenRepository.findByTokenHash("hash1");
        Optional<RefreshToken> retrieved2 = refreshTokenRepository.findByTokenHash("hash2");
        
        assertThat(retrieved1).isPresent();
        assertThat(retrieved1.get().getRevokedAt()).isNotNull();
        
        assertThat(retrieved2).isPresent();
        assertThat(retrieved2.get().getRevokedAt()).isNotNull();
    }
}
