package com.astral.express.pccms.user.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.common.helper.PasswordGenerator;
import com.astral.express.pccms.identity.repository.RefreshTokenRepository;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.notification.service.EmailService;
import com.astral.express.pccms.user.dto.request.AdminUpdateUserRequest;
import com.astral.express.pccms.user.dto.request.ChangePasswordRequest;
import com.astral.express.pccms.user.dto.request.CreateUserRequest;
import com.astral.express.pccms.user.dto.request.UserProfileUpdateRequest;
import com.astral.express.pccms.user.dto.response.AccountCredentialResponse;
import com.astral.express.pccms.user.dto.response.AccountResponse;
import com.astral.express.pccms.user.dto.response.UserResponse;
import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.StaffProfile;
import com.astral.express.pccms.user.entity.UserStatus;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.mapper.UserMapper;
import com.astral.express.pccms.user.repository.RoleRepository;
import com.astral.express.pccms.user.repository.StaffProfileRepository;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StaffProfileRepository staffProfileRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecurityContextService SecurityContextService;
    private final RefreshTokenRepository refreshTokenRepository;

    // ==================== ADMIN OPERATIONS ====================

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public com.astral.express.pccms.common.dto.PageResponse<AccountResponse> searchAccounts(
            String keyword,
            String role,
            UserStatus status,
            Pageable pageable) {
        Page<Users> users = userRepository.findAll(accountSearchSpecification(keyword, role, status), pageable);
        return com.astral.express.pccms.common.dto.PageResponse.of(users.map(this::toAccountResponse));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public AccountResponse updateAccountStatus(UUID accountId, UserStatus statusCode) {
        Users user = findActiveAccount(accountId);
        user.setStatusCode(statusCode);
        Users savedUser = userRepository.save(user);

        if (statusCode == UserStatus.LOCKED || statusCode == UserStatus.DISABLED) {
            refreshTokenRepository.revokeAllUserTokens(accountId);
        }

        log.info("Updated account status: {} -> {}", accountId, statusCode);
        return toAccountResponse(savedUser);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public AccountResponse assignAccountRole(UUID accountId, String roleCode) {
        Users user = findActiveAccount(accountId);
        Roles role = roleRepository.findByCodeIgnoreCase(roleCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_006_ROLE_NOT_FOUND));

        if (!Boolean.TRUE.equals(role.getIsActive())) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }

        user.setRole(role);
        Users savedUser = userRepository.save(user);
        log.info("Assigned account role: {} -> {}", accountId, role.getCode());
        return toAccountResponse(savedUser);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public AccountCredentialResponse createAccount(CreateUserRequest request) {
        Users savedUser = createUserEntity(request);
        String temporaryPassword = PasswordGenerator.generate(12);
        savedUser.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        savedUser = userRepository.save(savedUser);
        createStaffProfileIfNeeded(savedUser);
        emailService.sendAccountCreatedEmail(request.email(), temporaryPassword);

        log.info("Created new account: {}", savedUser.getEmail());
        return new AccountCredentialResponse(toAccountResponse(savedUser), temporaryPassword, true);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public UserResponse createUser(CreateUserRequest request) {
        Users user = createUserEntity(request);
        String plainPassword = PasswordGenerator.generate(8);
        user.setPasswordHash(passwordEncoder.encode(plainPassword));

        Users savedUser = userRepository.save(user);
        createStaffProfileIfNeeded(savedUser);
        emailService.sendAccountCreatedEmail(request.email(), plainPassword);

        log.info("Created new user: {}", savedUser.getEmail());
        return userMapper.toUserResponse(savedUser);
    }

    private Users createUserEntity(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.ERR_ACC_001_EMAIL_EXISTS);
        }

        Roles role = resolveActiveRole(request.roleCode());

        return Users.builder()
                .fullName(request.fullName())
                .email(request.email())
                .phone(normalize(request.phone()))
                .role(role)
                .statusCode(UserStatus.ACTIVE)
                .build();
    }

    private void createStaffProfileIfNeeded(Users user) {
        String roleCode = user.getRole() == null ? null : user.getRole().getCode();
        if (!"STAFF".equals(roleCode) && !"VETERINARIAN".equals(roleCode)) {
            return;
        }
        StaffProfile profile = StaffProfile.builder()
                .user(user)
                .professionalTitle(user.getRole().getName())
                .isServiceProvider("VETERINARIAN".equals(roleCode))
                .build();
        staffProfileRepository.save(profile);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public AccountResponse adminUpdateUser(UUID userId, AdminUpdateUserRequest request) {
        Users user = userRepository.findByIdWithRoleAndPermissions(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));

        String fullName = normalize(request.fullName());
        if (fullName != null) {
            user.setFullName(fullName);
        }

        String email = normalize(request.email());
        if (email != null && !email.equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new BusinessException(ErrorCode.ERR_ACC_001_EMAIL_EXISTS);
            }
            user.setEmail(email);
        }

        if (request.phone() != null) {
            user.setPhone(normalize(request.phone()));
        }

        if (request.roleCode() != null) {
            user.setRole(resolveActiveRole(request.roleCode()));
        }

        if (request.statusCode() != null) {
            user.setStatusCode(request.statusCode());
            if (request.statusCode() == UserStatus.LOCKED || request.statusCode() == UserStatus.DISABLED) {
                refreshTokenRepository.revokeAllUserTokens(userId);
            }
        }

        log.info("Admin updated user: {}", user.getEmail());
        return toAccountResponse(userRepository.save(user));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public AccountCredentialResponse resetAccountPassword(UUID accountId) {
        Users user = findActiveAccount(accountId);
        String temporaryPassword = PasswordGenerator.generate(12);
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        refreshTokenRepository.revokeAllUserTokens(accountId);
        Users savedUser = userRepository.save(user);
        emailService.sendTemporaryPasswordEmail(savedUser.getEmail(), temporaryPassword);

        log.info("Admin reset account password: {}", accountId);
        return new AccountCredentialResponse(toAccountResponse(savedUser), temporaryPassword, true);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public void adminLockUser(UUID id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
        user.setStatusCode(com.astral.express.pccms.user.entity.UserStatus.LOCKED);
        userRepository.save(user);
        refreshTokenRepository.revokeAllUserTokens(id);
        log.info("Locked user: {}", id);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public void adminDisableUser(UUID id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
        user.setStatusCode(com.astral.express.pccms.user.entity.UserStatus.DISABLED);
        userRepository.save(user);
        refreshTokenRepository.revokeAllUserTokens(id);
        log.info("Disabled user: {}", id);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public void deleteUser(UUID id) {
        Users user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
        user.setStatusCode(com.astral.express.pccms.user.entity.UserStatus.DISABLED);
        userRepository.save(user);
        log.info("Deleted (soft) user: {}", id);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public UserResponse getUser(UUID id) {
        Users user = userRepository.findByIdWithRoleAndPermissions(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
        return userMapper.toUserResponse(user);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream().map(userMapper::toUserResponse)
                .toList();
    }

    private Users findActiveAccount(UUID accountId) {
        Users user = userRepository.findByIdWithRoleAndPermissions(accountId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
        if (user.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND);
        }
        return user;
    }

    private AccountResponse toAccountResponse(Users user) {
        Roles role = user.getRole();
        String roleCode = role == null ? null : role.getCode();
        return new AccountResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getFullName(),
                roleCode,
                role == null ? null : role.getName(),
                roleCode == null ? List.of() : List.of(roleCode),
                user.getStatusCode(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Roles resolveActiveRole(String roleCode) {
        Roles role = roleRepository.findByCodeIgnoreCase(roleCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_006_ROLE_NOT_FOUND));

        if (!Boolean.TRUE.equals(role.getIsActive())) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }

        return role;
    }

    private Specification<Users> accountSearchSpecification(String keyword, String role, UserStatus status) {
        return combine(
                notDeleted(),
                accountKeywordContains(keyword),
                accountRoleEquals(role),
                accountStatusEquals(status)
        );
    }

    private Specification<Users> notDeleted() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNull(root.get("deletedAt"));
    }

    private Specification<Users> accountKeywordContains(String keyword) {
        String normalizedKeyword = normalize(keyword);
        if (normalizedKeyword == null) {
            return null;
        }
        String keywordLike = "%" + normalizedKeyword.toLowerCase(Locale.ROOT) + "%";
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), keywordLike),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), keywordLike),
                criteriaBuilder.like(root.get("phone"), "%" + normalizedKeyword + "%")
        );
    }

    private Specification<Users> accountRoleEquals(String role) {
        String normalizedRole = normalize(role);
        if (normalizedRole == null) {
            return null;
        }
        String roleCode = normalizedRole.toLowerCase(Locale.ROOT);
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(criteriaBuilder.lower(root.join("role").get("code")), roleCode);
    }

    private Specification<Users> accountStatusEquals(UserStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("statusCode"), status);
    }

    @SafeVarargs
    private final Specification<Users> combine(Specification<Users>... specifications) {
        List<Specification<Users>> activeSpecifications = new ArrayList<>();
        for (Specification<Users> specification : specifications) {
            if (specification != null) {
                activeSpecifications.add(specification);
            }
        }
        if (activeSpecifications.isEmpty()) {
            return null;
        }
        Specification<Users> combined = activeSpecifications.getFirst();
        for (int index = 1; index < activeSpecifications.size(); index++) {
            combined = combined.and(activeSpecifications.get(index));
        }
        return combined;
    }

    // ==================== USER SELF OPERATIONS ====================

    @PreAuthorize("isAuthenticated()")
    public UserResponse getMyProfile() {
        UUID userId = SecurityContextService.getCurrentUserId();
        Users user = userRepository.findByIdWithRoleAndPermissions(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
        return userMapper.toUserResponse(user);
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public UserResponse updateMyProfile(UserProfileUpdateRequest request) {
        UUID userId = SecurityContextService.getCurrentUserId();
        Users user = userRepository.findByIdWithRoleAndPermissions(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));

        userMapper.updateProfile(request, user);

        log.info("User updated profile: {}", user.getEmail());
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void changePassword(ChangePasswordRequest request) {
        UUID userId = SecurityContextService.getCurrentUserId();
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.ERR_IAM_001_INVALID_CREDENTIALS);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        log.info("User changed password: {}", user.getEmail());
    }
}
