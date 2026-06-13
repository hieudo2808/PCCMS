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
import com.astral.express.pccms.notification.service.EmailService;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.mapper.UserMapper;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OtpServiceTest {

    @Mock
    private OtpTokenRepository otpTokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SecurityContextService securityContextService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private OtpService otpService;

    @Test
    void should_RequestPasswordResetOtp_Success() {
        OtpRequest request = new OtpRequest("test@gmail.com");
        Users user = Users.builder().id(UUID.randomUUID()).email("test@gmail.com").build();
        given(userRepository.findByEmail("test@gmail.com")).willReturn(Optional.of(user));

        otpService.requestPasswordResetOtp(request);

        verify(otpTokenRepository).save(any(OtpToken.class));
        verify(emailService).sendOtpEmail(eq("test@gmail.com"), eq(OtpPurpose.RESET_PASSWORD.name()), anyString());
    }

    @Test
    void should_ConfirmPasswordReset_Success() {
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest("test@gmail.com", "123456", "new_pw");
        Users user = Users.builder().id(UUID.randomUUID()).email("test@gmail.com").build();
        given(userRepository.findByEmail("test@gmail.com")).willReturn(Optional.of(user));

        OtpToken token = OtpToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .contact("test@gmail.com")
                .purpose(OtpPurpose.RESET_PASSWORD)
                .tokenHash(hashOtp("123456"))
                .expiresAt(OffsetDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .build();
        given(otpTokenRepository.findFirstByContactAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.RESET_PASSWORD)).willReturn(Optional.of(token));

        given(passwordEncoder.encode("new_pw")).willReturn("hashed_new_pw");

        otpService.confirmPasswordReset(request);

        verify(userRepository).save(any(Users.class));
        verify(otpTokenRepository).save(any(OtpToken.class));
    }

    @Test
    void should_ThrowException_when_ConfirmPasswordResetWithWrongOtp() {
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest("test@gmail.com", "wrong", "new_pw");
        Users user = Users.builder().id(UUID.randomUUID()).email("test@gmail.com").build();
        given(userRepository.findByEmail("test@gmail.com")).willReturn(Optional.of(user));

        OtpToken token = OtpToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .contact("test@gmail.com")
                .purpose(OtpPurpose.RESET_PASSWORD)
                .tokenHash(hashOtp("123456"))
                .expiresAt(OffsetDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .build();
        given(otpTokenRepository.findFirstByContactAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.RESET_PASSWORD)).willReturn(Optional.of(token));

        assertThatThrownBy(() -> otpService.confirmPasswordReset(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_OTP_001_INVALID_OR_EXPIRED);
    }

    @Test
    void should_RequestEmailChangeOtp_Success() {
        UUID currentUserId = UUID.randomUUID();
        Users currentUser = Users.builder().id(currentUserId).email("old@gmail.com").build();
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(userRepository.findById(currentUserId)).willReturn(Optional.of(currentUser));
        
        OtpRequest request = new OtpRequest("new@gmail.com");
        given(userRepository.findByEmail("new@gmail.com")).willReturn(Optional.empty());

        otpService.requestEmailChangeOtp(request);

        verify(otpTokenRepository).save(any(OtpToken.class));
        verify(emailService).sendOtpEmail(eq("new@gmail.com"), eq(OtpPurpose.CHANGE_EMAIL.name()), anyString());
    }

    @Test
    void should_ConfirmEmailChange_Success() {
        UUID currentUserId = UUID.randomUUID();
        Users currentUser = Users.builder().id(currentUserId).email("old@gmail.com").build();
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(userRepository.findById(currentUserId)).willReturn(Optional.of(currentUser));

        OtpConfirmRequest request = new OtpConfirmRequest("new@gmail.com", "123456");
        
        OtpToken token = OtpToken.builder()
                .id(UUID.randomUUID())
                .user(currentUser)
                .contact("new@gmail.com")
                .purpose(OtpPurpose.CHANGE_EMAIL)
                .tokenHash(hashOtp("123456"))
                .expiresAt(OffsetDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .build();
        given(otpTokenRepository.findFirstByContactAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "new@gmail.com", OtpPurpose.CHANGE_EMAIL)).willReturn(Optional.of(token));

        otpService.confirmEmailChange(request);

        verify(userRepository).save(any(Users.class));
        verify(otpTokenRepository).save(any(OtpToken.class));
    }

    @Test
    void should_ThrowException_when_RequestPasswordResetUserNotFound() {
        OtpRequest request = new OtpRequest("notfound@gmail.com");
        given(userRepository.findByEmail("notfound@gmail.com")).willReturn(Optional.empty());
        assertThatThrownBy(() -> otpService.requestPasswordResetOtp(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ACC_002_USER_NOT_FOUND);
    }

    @Test
    void should_ThrowException_when_ConfirmPasswordResetMaxAttemptExceeded() {
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest("test@gmail.com", "wrong", "new_pw");
        Users user = Users.builder().id(UUID.randomUUID()).email("test@gmail.com").build();
        given(userRepository.findByEmail("test@gmail.com")).willReturn(Optional.of(user));

        OtpToken token = OtpToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .contact("test@gmail.com")
                .purpose(OtpPurpose.RESET_PASSWORD)
                .tokenHash(hashOtp("123456"))
                .expiresAt(OffsetDateTime.now().plusMinutes(5))
                .attemptCount(5) // Max attempt
                .build();
        given(otpTokenRepository.findFirstByContactAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.RESET_PASSWORD)).willReturn(Optional.of(token));

        assertThatThrownBy(() -> otpService.confirmPasswordReset(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_OTP_002_TOO_MANY_ATTEMPTS);
    }
    
    @Test
    void should_ThrowException_when_ConfirmPasswordResetExpired() {
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest("test@gmail.com", "123456", "new_pw");
        Users user = Users.builder().id(UUID.randomUUID()).email("test@gmail.com").build();
        given(userRepository.findByEmail("test@gmail.com")).willReturn(Optional.of(user));

        OtpToken token = OtpToken.builder()
                .id(UUID.randomUUID())
                .user(user)
                .contact("test@gmail.com")
                .purpose(OtpPurpose.RESET_PASSWORD)
                .tokenHash(hashOtp("123456"))
                .expiresAt(OffsetDateTime.now().minusMinutes(1)) // Expired
                .attemptCount(0)
                .build();
        given(otpTokenRepository.findFirstByContactAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "test@gmail.com", OtpPurpose.RESET_PASSWORD)).willReturn(Optional.of(token));

        assertThatThrownBy(() -> otpService.confirmPasswordReset(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_OTP_001_INVALID_OR_EXPIRED);
    }

    @Test
    void should_ThrowException_when_RequestEmailChangeEmailInUse() {
        UUID currentUserId = UUID.randomUUID();
        Users currentUser = Users.builder().id(currentUserId).email("old@gmail.com").build();
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(userRepository.findById(currentUserId)).willReturn(Optional.of(currentUser));
        
        OtpRequest request = new OtpRequest("new@gmail.com");
        Users existingUser = Users.builder().id(UUID.randomUUID()).build();
        given(userRepository.findByEmail("new@gmail.com")).willReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> otpService.requestEmailChangeOtp(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ACC_001_EMAIL_EXISTS);
    }

    private String hashOtp(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to hash OTP", e);
        }
    }

    @Test
    void should_RequestPhoneChangeOtp_Success() {
        UUID currentUserId = UUID.randomUUID();
        Users currentUser = Users.builder().id(currentUserId).phone("old_phone").build();
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(userRepository.findById(currentUserId)).willReturn(Optional.of(currentUser));
        
        OtpRequest request = new OtpRequest("new_phone");
        given(userRepository.findByPhone("new_phone")).willReturn(Optional.empty());

        otpService.requestPhoneChangeOtp(request);

        verify(otpTokenRepository).save(any(OtpToken.class));
        verify(emailService).sendOtpEmail(eq("new_phone"), eq(OtpPurpose.CHANGE_PHONE.name()), anyString());
    }

    @Test
    void should_ThrowException_when_RequestPhoneChangePhoneInUse() {
        UUID currentUserId = UUID.randomUUID();
        Users currentUser = Users.builder().id(currentUserId).phone("old_phone").build();
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(userRepository.findById(currentUserId)).willReturn(Optional.of(currentUser));
        
        OtpRequest request = new OtpRequest("new_phone");
        Users existingUser = Users.builder().id(UUID.randomUUID()).build();
        given(userRepository.findByPhone("new_phone")).willReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> otpService.requestPhoneChangeOtp(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ACC_008_PHONE_EXISTS);
    }

    @Test
    void should_ConfirmPhoneChange_Success() {
        UUID currentUserId = UUID.randomUUID();
        Users currentUser = Users.builder().id(currentUserId).phone("old_phone").build();
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(userRepository.findById(currentUserId)).willReturn(Optional.of(currentUser));

        OtpConfirmRequest request = new OtpConfirmRequest("new_phone", "123456");
        
        OtpToken token = OtpToken.builder()
                .id(UUID.randomUUID())
                .user(currentUser)
                .contact("new_phone")
                .purpose(OtpPurpose.CHANGE_PHONE)
                .tokenHash(hashOtp("123456"))
                .expiresAt(OffsetDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .build();
        given(otpTokenRepository.findFirstByContactAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "new_phone", OtpPurpose.CHANGE_PHONE)).willReturn(Optional.of(token));

        otpService.confirmPhoneChange(request);

        verify(userRepository).save(any(Users.class));
        verify(otpTokenRepository).save(any(OtpToken.class));
    }

    @Test
    void should_ThrowException_when_CurrentUserNull() {
        given(securityContextService.getCurrentUserId()).willReturn(null);
        OtpRequest request = new OtpRequest("new@gmail.com");

        assertThatThrownBy(() -> otpService.requestEmailChangeOtp(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_401_UNAUTHORIZED);
    }

    @Test
    void should_ThrowException_when_CurrentUserNotFoundInDb() {
        UUID currentUserId = UUID.randomUUID();
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(userRepository.findById(currentUserId)).willReturn(Optional.empty());

        OtpRequest request = new OtpRequest("new@gmail.com");

        assertThatThrownBy(() -> otpService.requestEmailChangeOtp(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ACC_002_USER_NOT_FOUND);
    }

    @Test
    void should_ThrowException_when_ContactIsBlank() {
        UUID currentUserId = UUID.randomUUID();
        Users currentUser = Users.builder().id(currentUserId).email("old@gmail.com").build();
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(userRepository.findById(currentUserId)).willReturn(Optional.of(currentUser));
        
        OtpRequest request = new OtpRequest("   ");

        assertThatThrownBy(() -> otpService.requestEmailChangeOtp(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void should_ThrowException_when_ConfirmForbiddenUser() {
        UUID currentUserId = UUID.randomUUID();
        Users currentUser = Users.builder().id(currentUserId).email("old@gmail.com").build();
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        given(userRepository.findById(currentUserId)).willReturn(Optional.of(currentUser));

        OtpConfirmRequest request = new OtpConfirmRequest("new@gmail.com", "123456");
        
        Users otherUser = Users.builder().id(UUID.randomUUID()).build();
        OtpToken token = OtpToken.builder()
                .id(UUID.randomUUID())
                .user(otherUser)
                .contact("new@gmail.com")
                .purpose(OtpPurpose.CHANGE_EMAIL)
                .tokenHash(hashOtp("123456"))
                .expiresAt(OffsetDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .build();
        given(otpTokenRepository.findFirstByContactAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(
                "new@gmail.com", OtpPurpose.CHANGE_EMAIL)).willReturn(Optional.of(token));

        assertThatThrownBy(() -> otpService.confirmEmailChange(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_403_FORBIDDEN);
    }
}
