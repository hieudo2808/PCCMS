package com.astral.express.pccms.user.service;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.repository.RefreshTokenRepository;
import com.astral.express.pccms.user.dto.request.AdminUpdateUserRequest;
import com.astral.express.pccms.user.dto.response.AccountResponse;
import com.astral.express.pccms.user.dto.response.UserResponse;
import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.UserStatus;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.mapper.UserMapper;
import com.astral.express.pccms.user.repository.RoleRepository;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountAdminService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccountResponseFactory accountResponseFactory;
    private final AccountProtectionPolicy accountProtectionPolicy;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public PageResponse<AccountResponse> searchAccounts(
            String keyword,
            String role,
            UserStatus status,
            Pageable pageable) {
        Page<Users> users = userRepository.findAll(accountSearchSpecification(keyword, role, status), pageable);
        return PageResponse.of(users.map(accountResponseFactory::toAccountResponse));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public AccountResponse updateAccountStatus(UUID accountId, UserStatus statusCode) {
        Users user = findActiveAccount(accountId);
        accountProtectionPolicy.assertNotProtectedAdmin(user);
        user.setStatusCode(statusCode);
        Users savedUser = userRepository.save(user);

        if (statusCode == UserStatus.LOCKED || statusCode == UserStatus.DISABLED) {
            refreshTokenRepository.revokeAllUserTokens(accountId);
        }

        log.info("Updated account status: {} -> {}", accountId, statusCode);
        return accountResponseFactory.toAccountResponse(savedUser);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public AccountResponse assignAccountRole(UUID accountId, String roleCode) {
        Users user = findActiveAccount(accountId);
        accountProtectionPolicy.assertNotProtectedAdmin(user);
        accountProtectionPolicy.assertNotAdminRoleRequest(roleCode);
        Roles role = roleRepository.findByCodeIgnoreCase(roleCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_006_ROLE_NOT_FOUND));

        if (!Boolean.TRUE.equals(role.getIsActive())) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }

        user.setRole(role);
        Users savedUser = userRepository.save(user);
        log.info("Assigned account role: {} -> {}", accountId, role.getCode());
        return accountResponseFactory.toAccountResponse(savedUser);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public AccountResponse adminUpdateUser(UUID userId, AdminUpdateUserRequest request) {
        Users user = userRepository.findByIdWithRoleAndPermissions(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
        accountProtectionPolicy.assertNotProtectedAdmin(user);

        String fullName = UserText.normalize(request.fullName());
        if (fullName != null) {
            user.setFullName(fullName);
        }

        String email = UserText.normalize(request.email());
        if (email != null && !email.equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new BusinessException(ErrorCode.ERR_ACC_001_EMAIL_EXISTS);
            }
            user.setEmail(email);
        }

        if (request.phone() != null) {
            user.setPhone(UserText.normalize(request.phone()));
        }

        if (request.roleCode() != null) {
            accountProtectionPolicy.assertNotAdminRoleRequest(request.roleCode());
            user.setRole(resolveActiveRole(request.roleCode()));
        }

        if (request.statusCode() != null) {
            user.setStatusCode(request.statusCode());
            if (request.statusCode() == UserStatus.LOCKED || request.statusCode() == UserStatus.DISABLED) {
                refreshTokenRepository.revokeAllUserTokens(userId);
            }
        }

        log.info("Admin updated user: {}", user.getEmail());
        return accountResponseFactory.toAccountResponse(userRepository.save(user));
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public void adminLockUser(UUID id) {
        Users user = userRepository.findByIdWithRoleAndPermissions(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
        accountProtectionPolicy.assertNotProtectedAdmin(user);
        user.setStatusCode(UserStatus.LOCKED);
        userRepository.save(user);
        refreshTokenRepository.revokeAllUserTokens(id);
        log.info("Locked user: {}", id);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public void adminDisableUser(UUID id) {
        Users user = userRepository.findByIdWithRoleAndPermissions(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
        accountProtectionPolicy.assertNotProtectedAdmin(user);
        user.setStatusCode(UserStatus.DISABLED);
        userRepository.save(user);
        refreshTokenRepository.revokeAllUserTokens(id);
        log.info("Disabled user: {}", id);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public void deleteUser(UUID id) {
        Users user = userRepository.findByIdWithRoleAndPermissions(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
        accountProtectionPolicy.assertNotProtectedAdmin(user);
        user.setStatusCode(UserStatus.DISABLED);
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

    private Roles resolveActiveRole(String roleCode) {
        accountProtectionPolicy.assertNotAdminRoleRequest(roleCode);
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
        String normalizedKeyword = UserText.normalize(keyword);
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
        String normalizedRole = UserText.normalize(role);
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
}
