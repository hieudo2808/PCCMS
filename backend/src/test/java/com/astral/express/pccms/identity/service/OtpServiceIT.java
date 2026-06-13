package com.astral.express.pccms.identity.service;

import com.astral.express.pccms.common.AbstractIntegrationTest;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.dto.request.OtpConfirmRequest;
import com.astral.express.pccms.identity.dto.request.OtpRequest;
import com.astral.express.pccms.identity.entity.OtpPurpose;
import com.astral.express.pccms.identity.entity.OtpToken;
import com.astral.express.pccms.identity.repository.OtpTokenRepository;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.notification.service.EmailService;
import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.UserStatus;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.RoleRepository;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

class OtpServiceIT extends AbstractIntegrationTest {

    @Autowired
    private OtpService otpService;

    @Autowired
    private OtpTokenRepository otpTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @MockitoBean
    private SecurityContextService securityContextService;

    @MockitoBean
    private EmailService emailService;

    private Users testUser;

    @BeforeEach
    void setUp() {
        otpTokenRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Roles role = Roles.builder()
                .code("CUSTOMER")
                .name("Khách hàng")
                .isActive(true)
                .build();
        roleRepository.saveAndFlush(role);

        testUser = Users.builder()
                .email("test.otp@pccms.vn")
                .passwordHash("hash")
                .fullName("Test OTP")
                .phone("0987654321")
                .role(role)
                .statusCode(UserStatus.ACTIVE)
                .build();
        testUser = userRepository.saveAndFlush(testUser);

        given(securityContextService.getCurrentUserId()).willReturn(testUser.getId());
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

    @Test
    void should_request_phone_change_otp_successfully() {
        // Act
        otpService.requestPhoneChangeOtp(new OtpRequest("0123456789"));

        // Assert
        long count = otpTokenRepository.count();
        assertThat(count).isEqualTo(1L);

        OtpToken token = otpTokenRepository.findAll().get(0);
        assertThat(token.getContact()).isEqualTo("0123456789");
        assertThat(token.getPurpose()).isEqualTo(OtpPurpose.CHANGE_PHONE);
        assertThat(token.getAttemptCount()).isZero();
    }

    @Test
    void should_throw_business_exception_if_phone_already_exists() {
        // Arrange
        Users otherUser = Users.builder()
                .email("other@pccms.vn")
                .passwordHash("hash")
                .phone("0123456789")
                .role(testUser.getRole())
                .statusCode(UserStatus.ACTIVE)
                .build();
        userRepository.saveAndFlush(otherUser);

        // Act & Assert
        assertThatThrownBy(() -> otpService.requestPhoneChangeOtp(new OtpRequest("0123456789")))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ACC_008_PHONE_EXISTS);
    }

    @Test
    void should_confirm_phone_change_successfully() {
        // Arrange
        String otp = "123456";
        OtpToken token = OtpToken.builder()
                .user(testUser)
                .contact("0123456789")
                .purpose(OtpPurpose.CHANGE_PHONE)
                .tokenHash(hash(otp))
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .attemptCount(0)
                .build();
        otpTokenRepository.saveAndFlush(token);

        // Act
        var response = otpService.confirmPhoneChange(new OtpConfirmRequest("0123456789", otp));

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.phone()).isEqualTo("0123456789");

        Users updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updated.getPhone()).isEqualTo("0123456789");
        assertThat(updated.getPhoneVerifiedAt()).isNotNull();

        OtpToken consumed = otpTokenRepository.findById(token.getId()).orElseThrow();
        assertThat(consumed.getConsumedAt()).isNotNull();
    }

    @Test
    void should_throw_business_exception_on_expired_otp() {
        // Arrange
        String otp = "123456";
        OtpToken token = OtpToken.builder()
                .user(testUser)
                .contact("0123456789")
                .purpose(OtpPurpose.CHANGE_PHONE)
                .tokenHash(hash(otp))
                .expiresAt(OffsetDateTime.now().minusMinutes(1)) // Expired
                .attemptCount(0)
                .build();
        otpTokenRepository.saveAndFlush(token);

        // Act & Assert
        assertThatThrownBy(() -> otpService.confirmPhoneChange(new OtpConfirmRequest("0123456789", otp)))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_OTP_001_INVALID_OR_EXPIRED);
    }

    @Test
    void should_increment_attempt_count_and_throw_on_wrong_otp() {
        // Arrange
        String otp = "123456";
        OtpToken token = OtpToken.builder()
                .user(testUser)
                .contact("0123456789")
                .purpose(OtpPurpose.CHANGE_PHONE)
                .tokenHash(hash(otp))
                .expiresAt(OffsetDateTime.now().plusMinutes(10))
                .attemptCount(0)
                .build();
        otpTokenRepository.saveAndFlush(token);

        // Act & Assert
        assertThatThrownBy(() -> otpService.confirmPhoneChange(new OtpConfirmRequest("0123456789", "654321"))) // Wrong OTP
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_OTP_001_INVALID_OR_EXPIRED);

        // Verify attempt count incremented
        OtpToken updated = otpTokenRepository.findById(token.getId()).orElseThrow();
        assertThat(updated.getAttemptCount()).isEqualTo(1);
        assertThat(updated.getConsumedAt()).isNull();
    }
}
