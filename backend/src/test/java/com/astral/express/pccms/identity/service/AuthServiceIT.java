package com.astral.express.pccms.identity.service;

import com.astral.express.pccms.common.AbstractIntegrationTest;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.dto.request.LoginRequest;
import com.astral.express.pccms.identity.dto.response.AuthResponse;
import com.astral.express.pccms.identity.entity.RefreshToken;
import com.astral.express.pccms.identity.repository.RefreshTokenRepository;
import com.astral.express.pccms.identity.security.JwtUtil;
import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.UserStatus;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.mapper.UserMapper;
import com.astral.express.pccms.user.repository.RoleRepository;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceIT extends AbstractIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private Users testUser;
    private Roles testRole;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        testRole = Roles.builder()
                .code("OWNER")
                .name("Owner")
                .isActive(true)
                .build();
        testRole = roleRepository.saveAndFlush(testRole);

        testUser = Users.builder()
                .email("test.auth@pccms.vn")
                .passwordHash(passwordEncoder.encode("password123"))
                .fullName("Test Auth")
                .role(testRole)
                .statusCode(UserStatus.ACTIVE)
                .build();
        testUser = userRepository.saveAndFlush(testUser);
    }

    @Test
    void should_login_successfully() {
        // Arrange
        LoginRequest request = new LoginRequest("test.auth@pccms.vn", "password123");

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        
        long tokenCount = refreshTokenRepository.count();
        assertThat(tokenCount).isEqualTo(1L);
    }

    @Test
    void should_throw_business_exception_on_invalid_credentials() {
        // Arrange
        LoginRequest request = new LoginRequest("test.auth@pccms.vn", "wrongpassword");

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_IAM_001_INVALID_CREDENTIALS);
    }

    @Test
    void should_throw_business_exception_when_account_locked() {
        // Arrange
        testUser.setStatusCode(UserStatus.LOCKED);
        userRepository.saveAndFlush(testUser);

        LoginRequest request = new LoginRequest("test.auth@pccms.vn", "password123");

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_IAM_002_ACCOUNT_LOCKED);
    }

    @Test
    void should_refresh_token_successfully() {
        // Arrange
        LoginRequest request = new LoginRequest("test.auth@pccms.vn", "password123");
        AuthResponse loginResponse = authService.login(request);
        String refreshToken = loginResponse.getRefreshToken();

        // Act
        AuthResponse refreshResponse = authService.refreshAccessToken(refreshToken);

        // Assert
        assertThat(refreshResponse).isNotNull();
        assertThat(refreshResponse.getToken()).isNotBlank();
        assertThat(refreshResponse.getRefreshToken()).isNotBlank();

        // Original token should be revoked
        long activeTokens = refreshTokenRepository.findAll().stream()
                .filter(t -> t.getRevokedAt() == null)
                .count();
        assertThat(activeTokens).isEqualTo(1L);
    }
}
