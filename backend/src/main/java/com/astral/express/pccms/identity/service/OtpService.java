package com.astral.express.pccms.identity.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.dto.request.OtpConfirmRequest;
import com.astral.express.pccms.identity.dto.request.OtpRequest;
import com.astral.express.pccms.identity.dto.request.PasswordResetConfirmRequest;
import com.astral.express.pccms.identity.entity.OtpPurpose;
import com.astral.express.pccms.identity.entity.OtpToken;
import com.astral.express.pccms.identity.repository.OtpTokenRepository;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.identity.service.OtpService;
import com.astral.express.pccms.notification.service.EmailService;
import com.astral.express.pccms.user.dto.response.UserResponse;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.mapper.UserMapper;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OtpService {
    private static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpTokenRepository otpTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextService SecurityContextService;
    private final UserMapper userMapper;
    private final EmailService emailService;
@Transactional
    public void requestPasswordResetOtp(OtpRequest request) {
        Users user = userRepository.findByEmail(normalizeEmail(request.contact()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
        createOtp(user, user.getEmail(), OtpPurpose.RESET_PASSWORD);
    }
@Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        Users user = userRepository.findByEmail(normalizeEmail(request.contact()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
        consumeOtp(user.getEmail(), OtpPurpose.RESET_PASSWORD, request.otp(), user.getId());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
@Transactional
    public void requestEmailChangeOtp(OtpRequest request) {
        Users currentUser = currentUser();
        String newEmail = normalizeEmail(request.contact());
        userRepository.findByEmail(newEmail)
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .ifPresent(user -> {
                    throw new BusinessException(ErrorCode.ERR_ACC_001_EMAIL_EXISTS);
                });
        createOtp(currentUser, newEmail, OtpPurpose.CHANGE_EMAIL);
    }
@Transactional
    public UserResponse confirmEmailChange(OtpConfirmRequest request) {
        Users currentUser = currentUser();
        String newEmail = normalizeEmail(request.contact());
        consumeOtp(newEmail, OtpPurpose.CHANGE_EMAIL, request.otp(), currentUser.getId());
        currentUser.setEmail(newEmail);
        currentUser.setEmailVerifiedAt(OffsetDateTime.now());
        return userMapper.toUserResponse(userRepository.save(currentUser));
    }
@Transactional
    public void requestPhoneChangeOtp(OtpRequest request) {
        Users currentUser = currentUser();
        String newPhone = normalizeContact(request.contact());
        userRepository.findByPhone(newPhone)
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .ifPresent(user -> {
                    throw new BusinessException(ErrorCode.ERR_ACC_008_PHONE_EXISTS);
                });
        createOtp(currentUser, newPhone, OtpPurpose.CHANGE_PHONE);
    }
@Transactional
    public UserResponse confirmPhoneChange(OtpConfirmRequest request) {
        Users currentUser = currentUser();
        String newPhone = normalizeContact(request.contact());
        consumeOtp(newPhone, OtpPurpose.CHANGE_PHONE, request.otp(), currentUser.getId());
        currentUser.setPhone(newPhone);
        currentUser.setPhoneVerifiedAt(OffsetDateTime.now());
        return userMapper.toUserResponse(userRepository.save(currentUser));
    }

    private void createOtp(Users user, String contact, OtpPurpose purpose) {
        String otp = "%06d".formatted(RANDOM.nextInt(1_000_000));
        otpTokenRepository.save(OtpToken.builder()
                .user(user)
                .contact(contact)
                .purpose(purpose)
                .tokenHash(hash(otp))
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .attemptCount(0)
                .build());
        emailService.sendOtpEmail(contact, purpose.name(), otp);
    }

    private void consumeOtp(String contact, OtpPurpose purpose, String otp, UUID expectedUserId) {
        OtpToken token = otpTokenRepository
                .findFirstByContactAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(contact, purpose)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_OTP_001_INVALID_OR_EXPIRED));

        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.ERR_OTP_001_INVALID_OR_EXPIRED);
        }
        if (token.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new BusinessException(ErrorCode.ERR_OTP_002_TOO_MANY_ATTEMPTS);
        }
        if (expectedUserId != null && token.getUser() != null && !expectedUserId.equals(token.getUser().getId())) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }
        if (!hash(otp).equals(token.getTokenHash())) {
            token.setAttemptCount(token.getAttemptCount() + 1);
            otpTokenRepository.save(token);
            throw new BusinessException(ErrorCode.ERR_OTP_001_INVALID_OR_EXPIRED);
        }

        token.setConsumedAt(OffsetDateTime.now());
        otpTokenRepository.save(token);
    }

    private Users currentUser() {
        UUID userId = SecurityContextService.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.ERR_401_UNAUTHORIZED);
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
    }

    private String normalizeEmail(String value) {
        return normalizeContact(value).toLowerCase(Locale.ROOT);
    }

    private String normalizeContact(String value) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        return value.trim();
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to hash OTP", e);
        }
    }
}


