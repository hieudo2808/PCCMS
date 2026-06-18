package com.astral.express.pccms.user.service;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.user.dto.request.AdminUpdateUserRequest;
import com.astral.express.pccms.user.dto.request.ChangePasswordRequest;
import com.astral.express.pccms.user.dto.request.CreateUserRequest;
import com.astral.express.pccms.user.dto.request.UserProfileUpdateRequest;
import com.astral.express.pccms.user.dto.response.AccountCredentialResponse;
import com.astral.express.pccms.user.dto.response.AccountResponse;
import com.astral.express.pccms.user.dto.response.UserResponse;
import com.astral.express.pccms.user.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final AccountAdminService accountAdminService;
    private final AccountCredentialService accountCredentialService;
    private final UserProfileService userProfileService;

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public PageResponse<AccountResponse> searchAccounts(
            String keyword,
            String role,
            UserStatus status,
            Pageable pageable) {
        return accountAdminService.searchAccounts(keyword, role, status, pageable);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public AccountResponse updateAccountStatus(UUID accountId, UserStatus statusCode) {
        return accountAdminService.updateAccountStatus(accountId, statusCode);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public AccountResponse assignAccountRole(UUID accountId, String roleCode) {
        return accountAdminService.assignAccountRole(accountId, roleCode);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public AccountCredentialResponse createAccount(CreateUserRequest request) {
        return accountCredentialService.createAccount(request);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public UserResponse createUser(CreateUserRequest request) {
        return accountCredentialService.createUser(request);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public AccountResponse adminUpdateUser(UUID userId, AdminUpdateUserRequest request) {
        return accountAdminService.adminUpdateUser(userId, request);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public AccountCredentialResponse resetAccountPassword(UUID accountId) {
        return accountCredentialService.resetAccountPassword(accountId);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public void adminLockUser(UUID id) {
        accountAdminService.adminLockUser(id);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public void adminDisableUser(UUID id) {
        accountAdminService.adminDisableUser(id);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public void deleteUser(UUID id) {
        accountAdminService.deleteUser(id);
    }

    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public UserResponse getUser(UUID id) {
        return accountAdminService.getUser(id);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public List<UserResponse> getAllUsers() {
        return accountAdminService.getAllUsers();
    }

    @PreAuthorize("isAuthenticated()")
    public UserResponse getMyProfile() {
        return userProfileService.getMyProfile();
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public UserResponse updateMyProfile(UserProfileUpdateRequest request) {
        return userProfileService.updateMyProfile(request);
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void changePassword(ChangePasswordRequest request) {
        userProfileService.changePassword(request);
    }
}
