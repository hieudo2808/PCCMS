package com.astral.express.pccms.identity.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.dto.request.LoginRequest;
import com.astral.express.pccms.identity.dto.request.RegisterRequest;
import com.astral.express.pccms.identity.dto.response.AuthResponse;
import com.astral.express.pccms.identity.entity.RefreshToken;
import com.astral.express.pccms.identity.repository.RefreshTokenRepository;
import com.astral.express.pccms.identity.security.JwtUtil;
import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.entity.UserStatus;
import com.astral.express.pccms.user.mapper.UserMapper;
import com.astral.express.pccms.user.repository.RoleRepository;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final TokenBlacklistService tokenBlacklistService;

    private static final String DEFAULT_ROLE = "OWNER";

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.ERR_ACC_001_EMAIL_EXISTS);
        }

        // Get default role
        Roles role = roleRepository.findByCode(DEFAULT_ROLE)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_005_DEFAULT_ROLE_NOT_FOUND));

        // Create new user
        Users user = Users.builder()
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(role)
                .statusCode(UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (BadCredentialsException e) {
            throw new BusinessException(ErrorCode.ERR_IAM_001_INVALID_CREDENTIALS);
        } catch (org.springframework.security.authentication.InternalAuthenticationServiceException e) {
            if (e.getCause() instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.ERR_500_INTERNAL_SERVER);
        }

        Users user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));

        if (user.getStatusCode() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ERR_IAM_002_ACCOUNT_LOCKED);
        }

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshAccessToken(String refreshToken) {
        String tokenHash = hashToken(refreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_401_UNAUTHORIZED));

        if (storedToken.getRevokedAt() != null || storedToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.ERR_401_UNAUTHORIZED);
        }

        // Revoke old token
        storedToken.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(storedToken);

        // Generate new tokens
        return generateAuthResponse(storedToken.getUser());
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                String jti = jwtUtil.extractJti(accessToken);
                long expirationMs = jwtUtil.extractExpiration(accessToken).getTime() - System.currentTimeMillis();
                if (expirationMs > 0) {
                    tokenBlacklistService.blacklist(jti, expirationMs);
                }
            } catch (Exception e) {
                log.debug(e.getMessage());
                log.info("Revoke already");
            }
        }

        String tokenHash = hashToken(refreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(token -> {
                    token.setRevokedAt(OffsetDateTime.now());
                    refreshTokenRepository.save(token);
                });
    }

    private AuthResponse generateAuthResponse(Users user) {
        String accessToken = jwtUtil.generateToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        // Store refresh token
        RefreshToken tokenEntity = RefreshToken.builder()
                .tokenHash(hashToken(refreshToken))
                .user(user)
                .issuedAt(OffsetDateTime.now())
                .expiresAt(OffsetDateTime.now().plusSeconds(jwtUtil.getRefreshExpiration() / 1000))
                .revokedAt(null)
                .build();
        refreshTokenRepository.save(tokenEntity);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .user(userMapper.toUserResponse(user))
                .build();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}
