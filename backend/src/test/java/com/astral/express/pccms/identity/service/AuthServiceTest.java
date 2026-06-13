package com.astral.express.pccms.identity.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.dto.request.LoginRequest;
import com.astral.express.pccms.identity.dto.request.RegisterRequest;
import com.astral.express.pccms.identity.dto.response.AuthResponse;
import com.astral.express.pccms.identity.entity.RefreshToken;
import com.astral.express.pccms.identity.repository.RefreshTokenRepository;
import com.astral.express.pccms.identity.security.JwtUtil;
import com.astral.express.pccms.user.dto.response.UserResponse;
import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.UserStatus;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.mapper.UserMapper;
import com.astral.express.pccms.user.repository.RoleRepository;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserMapper userMapper;
    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private AuthService authService;

    @Test
    void should_Register_when_DataIsValid() {
        // GIVEN
        RegisterRequest request = new RegisterRequest("test@gmail.com", "password", "Test User");
        given(userRepository.existsByEmail(request.email())).willReturn(false);

        Roles role = Roles.builder().id(UUID.randomUUID()).code("OWNER").build();
        given(roleRepository.findByCode("OWNER")).willReturn(Optional.of(role));

        given(passwordEncoder.encode(request.password())).willReturn("hashed_pw");

        Users savedUser = Users.builder().id(UUID.randomUUID()).email(request.email()).build();
        given(userRepository.save(any(Users.class))).willReturn(savedUser);

        given(jwtUtil.generateToken(any())).willReturn("access_token");
        given(jwtUtil.generateRefreshToken(any())).willReturn("refresh_token");
        given(jwtUtil.getRefreshExpiration()).willReturn(86400000L);

        UserResponse userResponse = new UserResponse(savedUser.getId(), "Test User", request.email(), "0123456789", "OWNER", OffsetDateTime.now(), UserStatus.ACTIVE);
        given(userMapper.toUserResponse(any())).willReturn(userResponse);

        // WHEN
        AuthResponse response = authService.register(request);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("access_token");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void should_ThrowException_when_RegisterWithExistingEmail() {
        // GIVEN
        RegisterRequest request = new RegisterRequest("test@gmail.com", "password", "Test User");
        given(userRepository.existsByEmail(request.email())).willReturn(true);

        // WHEN & THEN
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ACC_001_EMAIL_EXISTS);
    }

    @Test
    void should_Login_when_CredentialsAreValid() {
        // GIVEN
        LoginRequest request = new LoginRequest("test@gmail.com", "password");
        Users user = Users.builder().id(UUID.randomUUID()).email(request.email()).statusCode(UserStatus.ACTIVE).build();
        
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(null);
        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));

        given(jwtUtil.generateToken(any())).willReturn("access_token");
        given(jwtUtil.generateRefreshToken(any())).willReturn("refresh_token");
        given(jwtUtil.getRefreshExpiration()).willReturn(86400000L);
        
        UserResponse userResponse = new UserResponse(user.getId(), "Test User", request.email(), "0123456789", "OWNER", OffsetDateTime.now(), UserStatus.ACTIVE);
        given(userMapper.toUserResponse(any())).willReturn(userResponse);

        // WHEN
        AuthResponse response = authService.login(request);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("access_token");
    }

    @Test
    void should_ThrowException_when_LoginWithInvalidCredentials() {
        // GIVEN
        LoginRequest request = new LoginRequest("test@gmail.com", "wrong_password");
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willThrow(new BadCredentialsException("Bad credentials"));

        // WHEN & THEN
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_IAM_001_INVALID_CREDENTIALS);
    }

    @Test
    void should_ThrowException_when_LoginWithInactiveAccount() {
        // GIVEN
        LoginRequest request = new LoginRequest("test@gmail.com", "password");
        Users user = Users.builder().id(UUID.randomUUID()).email(request.email()).statusCode(UserStatus.LOCKED).build();
        
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(null);
        given(userRepository.findByEmail(request.email())).willReturn(Optional.of(user));

        // WHEN & THEN
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_IAM_002_ACCOUNT_LOCKED);
    }

    @Test
    void should_RefreshAccessToken_when_TokenIsValid() {
        // GIVEN
        String refreshToken = "valid_refresh_token";
        Users user = Users.builder().id(UUID.randomUUID()).build();
        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .build();
        
        given(refreshTokenRepository.findByTokenHash(any())).willReturn(Optional.of(storedToken));
        given(jwtUtil.generateToken(any())).willReturn("new_access_token");
        given(jwtUtil.generateRefreshToken(any())).willReturn("new_refresh_token");
        given(jwtUtil.getRefreshExpiration()).willReturn(86400000L);
        
        // WHEN
        AuthResponse response = authService.refreshAccessToken(refreshToken);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("new_access_token");
        assertThat(storedToken.getRevokedAt()).isNotNull(); // Ensure old token was revoked
    }

    @Test
    void should_ThrowException_when_RefreshAccessTokenWithRevokedToken() {
        // GIVEN
        String refreshToken = "revoked_token";
        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .revokedAt(OffsetDateTime.now().minusDays(1))
                .build();
        
        given(refreshTokenRepository.findByTokenHash(any())).willReturn(Optional.of(storedToken));

        // WHEN & THEN
        assertThatThrownBy(() -> authService.refreshAccessToken(refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_401_UNAUTHORIZED);
    }

    @Test
    void should_Logout_when_ValidTokensProvided() {
        // GIVEN
        String accessToken = "valid_access_token";
        String refreshToken = "valid_refresh_token";
        
        given(jwtUtil.extractJti(accessToken)).willReturn("jti");
        given(jwtUtil.extractExpiration(accessToken)).willReturn(new Date(System.currentTimeMillis() + 10000));
        
        RefreshToken storedToken = RefreshToken.builder().id(UUID.randomUUID()).build();
        given(refreshTokenRepository.findByTokenHash(any())).willReturn(Optional.of(storedToken));

        // WHEN
        authService.logout(accessToken, refreshToken);

        // THEN
        verify(tokenBlacklistService).blacklist(any(), org.mockito.ArgumentMatchers.anyLong());
        verify(refreshTokenRepository).save(storedToken);
        assertThat(storedToken.getRevokedAt()).isNotNull();
    }

    @Test
    void should_ThrowException_when_InternalAuthenticationServiceException_withBusinessException() {
        LoginRequest request = new LoginRequest("test@gmail.com", "password");
        BusinessException cause = new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND);
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willThrow(new org.springframework.security.authentication.InternalAuthenticationServiceException("error", cause));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ACC_002_USER_NOT_FOUND);
    }

    @Test
    void should_ThrowException_when_InternalAuthenticationServiceException_withoutBusinessException() {
        LoginRequest request = new LoginRequest("test@gmail.com", "password");
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willThrow(new org.springframework.security.authentication.InternalAuthenticationServiceException("error"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_500_INTERNAL_SERVER);
    }

    @Test
    void should_ThrowException_when_RefreshAccessTokenExpired() {
        String refreshToken = "expired_token";
        RefreshToken storedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .expiresAt(OffsetDateTime.now().minusDays(1))
                .build();
        
        given(refreshTokenRepository.findByTokenHash(any())).willReturn(Optional.of(storedToken));

        assertThatThrownBy(() -> authService.refreshAccessToken(refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_401_UNAUTHORIZED);
    }

    @Test
    void should_Logout_when_AccessTokenNull() {
        String refreshToken = "valid_refresh_token";
        RefreshToken storedToken = RefreshToken.builder().id(UUID.randomUUID()).build();
        given(refreshTokenRepository.findByTokenHash(any())).willReturn(Optional.of(storedToken));

        authService.logout(null, refreshToken);

        verify(refreshTokenRepository).save(storedToken);
        assertThat(storedToken.getRevokedAt()).isNotNull();
    }

    @Test
    void should_Logout_when_AccessTokenException() {
        String accessToken = "invalid_access_token";
        String refreshToken = "valid_refresh_token";
        
        given(jwtUtil.extractJti(accessToken)).willThrow(new RuntimeException("invalid token"));
        
        RefreshToken storedToken = RefreshToken.builder().id(UUID.randomUUID()).build();
        given(refreshTokenRepository.findByTokenHash(any())).willReturn(Optional.of(storedToken));

        authService.logout(accessToken, refreshToken);

        verify(refreshTokenRepository).save(storedToken);
        assertThat(storedToken.getRevokedAt()).isNotNull();
    }
}
