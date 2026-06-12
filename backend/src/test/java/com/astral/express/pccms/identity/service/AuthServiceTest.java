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
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    @ParameterizedTest
    @CsvFileSource(resources = "/testcases/auth-service-testcases.csv", numLinesToSkip = 1)
    void executeAuthServiceTests(String ruleId, String caseId, String action, String mockState, String expectedResult, ErrorCode expectedError, String note) {
        // GIVEN
        UUID userId = UUID.randomUUID();
        
        Roles mockRole = Roles.builder().code("OWNER").name("Owner").build();
        
        Users mockUser = Users.builder()
                .id(userId)
                .email("test@pccms.vn")
                .passwordHash("hashedPass")
                .fullName("Test User")
                .role(mockRole)
                .statusCode(UserStatus.ACTIVE)
                .build();
                
        RegisterRequest registerReq = new RegisterRequest("Test User", "test@pccms.vn", "password");
        LoginRequest loginReq = new LoginRequest("test@pccms.vn", "password");
        
        UserResponse mockUserResp = new UserResponse(userId, "Test User", "test@pccms.vn", "0123456789", "OWNER", null, UserStatus.ACTIVE);
        
        String dummyToken = "accessToken";
        String dummyRefresh = "refreshToken";
        
        switch (action) {
            case "REGISTER":
                if ("VALID".equals(mockState)) {
                    given(userRepository.existsByEmail(registerReq.email())).willReturn(false);
                    given(roleRepository.findByCode("OWNER")).willReturn(Optional.of(mockRole));
                    given(passwordEncoder.encode(registerReq.password())).willReturn("hashedPass");
                    given(userRepository.save(any(Users.class))).willReturn(mockUser);
                    given(jwtUtil.generateToken(mockUser)).willReturn(dummyToken);
                    given(jwtUtil.generateRefreshToken(mockUser)).willReturn(dummyRefresh);
                    given(jwtUtil.getRefreshExpiration()).willReturn(86400000L);
                    given(userMapper.toUserResponse(mockUser)).willReturn(mockUserResp);
                    given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(inv -> inv.getArgument(0));
                } else if ("EMAIL_EXISTS".equals(mockState)) {
                    given(userRepository.existsByEmail(registerReq.email())).willReturn(true);
                } else if ("ROLE_NOT_FOUND".equals(mockState)) {
                    given(userRepository.existsByEmail(registerReq.email())).willReturn(false);
                    given(roleRepository.findByCode("OWNER")).willReturn(Optional.empty());
                }
                break;
                
            case "LOGIN":
                if ("VALID".equals(mockState)) {
                    given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(null);
                    given(userRepository.findByEmail(loginReq.email())).willReturn(Optional.of(mockUser));
                    given(jwtUtil.generateToken(mockUser)).willReturn(dummyToken);
                    given(jwtUtil.generateRefreshToken(mockUser)).willReturn(dummyRefresh);
                    given(jwtUtil.getRefreshExpiration()).willReturn(86400000L);
                    given(userMapper.toUserResponse(mockUser)).willReturn(mockUserResp);
                    given(refreshTokenRepository.save(any(RefreshToken.class))).willAnswer(inv -> inv.getArgument(0));
                } else if ("BAD_CREDENTIALS".equals(mockState)) {
                    given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                        .willThrow(new BadCredentialsException("Bad credentials"));
                } else if ("USER_NOT_FOUND".equals(mockState)) {
                    given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(null);
                    given(userRepository.findByEmail(loginReq.email())).willReturn(Optional.empty());
                } else if ("ACCOUNT_LOCKED".equals(mockState)) {
                    given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(null);
                    Users lockedUser = Users.builder().statusCode(UserStatus.LOCKED).build();
                    given(userRepository.findByEmail(loginReq.email())).willReturn(Optional.of(lockedUser));
                }
                break;
                
            case "REFRESH":
                if ("VALID".equals(mockState)) {
                    RefreshToken rt = RefreshToken.builder().user(mockUser).revokedAt(null).expiresAt(OffsetDateTime.now().plusDays(1)).build();
                    given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(rt));
                    given(jwtUtil.generateToken(mockUser)).willReturn(dummyToken);
                    given(jwtUtil.generateRefreshToken(mockUser)).willReturn(dummyRefresh);
                    given(jwtUtil.getRefreshExpiration()).willReturn(86400000L);
                    given(userMapper.toUserResponse(mockUser)).willReturn(mockUserResp);
                } else if ("TOKEN_NOT_FOUND".equals(mockState)) {
                    given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.empty());
                } else if ("TOKEN_REVOKED".equals(mockState)) {
                    RefreshToken rt = RefreshToken.builder().user(mockUser).revokedAt(OffsetDateTime.now()).expiresAt(OffsetDateTime.now().plusDays(1)).build();
                    given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(rt));
                } else if ("TOKEN_EXPIRED".equals(mockState)) {
                    RefreshToken rt = RefreshToken.builder().user(mockUser).revokedAt(null).expiresAt(OffsetDateTime.now().minusDays(1)).build();
                    given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(rt));
                }
                break;
                
            case "LOGOUT":
                given(jwtUtil.extractJti(dummyToken)).willReturn("jti");
                given(jwtUtil.extractExpiration(dummyToken)).willReturn(new Date(System.currentTimeMillis() + 10000));
                RefreshToken rt = RefreshToken.builder().user(mockUser).revokedAt(null).build();
                given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(rt));
                break;
        }
        
        // WHEN & THEN
        if ("EXCEPTION".equals(expectedResult)) {
            assertThatThrownBy(() -> {
                switch (action) {
                    case "REGISTER": authService.register(registerReq); break;
                    case "LOGIN": authService.login(loginReq); break;
                    case "REFRESH": authService.refreshAccessToken("dummyRefresh"); break;
                }
            }).isInstanceOf(BusinessException.class)
              .hasFieldOrPropertyWithValue("errorCode", expectedError);
        } else {
            switch (action) {
                case "REGISTER":
                    AuthResponse regResp = authService.register(registerReq);
                    assertThat(regResp.getToken()).isEqualTo(dummyToken);
                    break;
                case "LOGIN":
                    AuthResponse loginResp = authService.login(loginReq);
                    assertThat(loginResp.getToken()).isEqualTo(dummyToken);
                    break;
                case "REFRESH":
                    AuthResponse refreshResp = authService.refreshAccessToken("dummyRefresh");
                    assertThat(refreshResp.getToken()).isEqualTo(dummyToken);
                    break;
                case "LOGOUT":
                    authService.logout(dummyToken, dummyRefresh);
                    verify(tokenBlacklistService).blacklist(eq("jti"), anyLong());
                    verify(refreshTokenRepository).save(argThat(rt -> rt.getRevokedAt() != null));
                    break;
            }
        }
    }
}
