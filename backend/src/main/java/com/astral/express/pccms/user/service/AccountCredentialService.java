package com.astral.express.pccms.user.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.common.helper.PasswordGenerator;
import com.astral.express.pccms.identity.repository.RefreshTokenRepository;
import com.astral.express.pccms.notification.service.EmailService;
import com.astral.express.pccms.notification.service.NotificationService;
import com.astral.express.pccms.user.dto.request.CreateUserRequest;
import com.astral.express.pccms.user.dto.response.AccountCredentialResponse;
import com.astral.express.pccms.user.dto.response.UserResponse;
import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.UserStatus;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.mapper.UserMapper;
import com.astral.express.pccms.user.repository.RoleRepository;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountCredentialService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StaffProfileProvisioningService staffProfileProvisioningService;
    private final AccountResponseFactory accountResponseFactory;
    private final AccountProtectionPolicy accountProtectionPolicy;

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public AccountCredentialResponse createAccount(CreateUserRequest request) {
        Users savedUser = createUserEntity(request);
        String temporaryPassword = PasswordGenerator.generate(12);
        savedUser.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        savedUser = userRepository.save(savedUser);
        staffProfileProvisioningService.createStaffProfileIfNeeded(savedUser);
        emailService.sendAccountCreatedEmail(request.email(), temporaryPassword);
        notificationService.createNotification(savedUser.getId(), "ACCOUNT", savedUser.getId(), "ACCOUNT_CREATED",
                "Tài khoản đã được tạo", "Tài khoản của bạn đã được tạo. Vui lòng kiểm tra email để nhận thông tin đăng nhập.");

        log.info("Created new account: {}", savedUser.getEmail());
        return new AccountCredentialResponse(accountResponseFactory.toAccountResponse(savedUser), temporaryPassword, true);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public UserResponse createUser(CreateUserRequest request) {
        Users user = createUserEntity(request);
        String plainPassword = PasswordGenerator.generate(8);
        user.setPasswordHash(passwordEncoder.encode(plainPassword));

        Users savedUser = userRepository.save(user);
        staffProfileProvisioningService.createStaffProfileIfNeeded(savedUser);
        emailService.sendAccountCreatedEmail(request.email(), plainPassword);
        notificationService.createNotification(savedUser.getId(), "ACCOUNT", savedUser.getId(), "ACCOUNT_CREATED",
                "Tài khoản đã được tạo", "Tài khoản của bạn đã được tạo. Vui lòng kiểm tra email để nhận thông tin đăng nhập.");

        log.info("Created new user: {}", savedUser.getEmail());
        return userMapper.toUserResponse(savedUser);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public AccountCredentialResponse resetAccountPassword(UUID accountId) {
        Users user = findActiveAccount(accountId);
        accountProtectionPolicy.assertNotProtectedAdmin(user);
        String temporaryPassword = PasswordGenerator.generate(12);
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        refreshTokenRepository.revokeAllUserTokens(accountId);
        Users savedUser = userRepository.save(user);
        emailService.sendTemporaryPasswordEmail(savedUser.getEmail(), temporaryPassword);
        notificationService.createNotification(savedUser.getId(), "ACCOUNT", savedUser.getId(), "PASSWORD_RESET",
                "Mật khẩu đã được đặt lại", "Admin đã tạo mật khẩu tạm thời cho tài khoản của bạn. Vui lòng kiểm tra email.");

        log.info("Admin reset account password: {}", accountId);
        return new AccountCredentialResponse(accountResponseFactory.toAccountResponse(savedUser), temporaryPassword, true);
    }

    private Users createUserEntity(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.ERR_ACC_001_EMAIL_EXISTS);
        }

        Roles role = resolveActiveRole(request.roleCode());

        return Users.builder()
                .fullName(request.fullName())
                .email(request.email())
                .phone(UserText.normalize(request.phone()))
                .role(role)
                .statusCode(UserStatus.ACTIVE)
                .build();
    }

    private Users findActiveAccount(UUID accountId) {
        Users user = userRepository.findByIdWithRoleAndPermissions(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
        if (user.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND);
        }
        return user;
    }

    private Roles resolveActiveRole(String roleCode) {
        accountProtectionPolicy.assertNotAdminRoleRequest(roleCode);
        Roles role = roleRepository.findByCodeIgnoreCase(roleCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_006_ROLE_NOT_FOUND));

        if (!Boolean.TRUE.equals(role.getIsActive())) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }

        return role;
    }
}
