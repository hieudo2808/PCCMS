package com.astral.express.pccms.user.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.user.dto.request.AdminUpdateUserRequest;
import com.astral.express.pccms.user.dto.request.AssignAccountRoleRequest;
import com.astral.express.pccms.user.dto.request.CreateUserRequest;
import com.astral.express.pccms.user.dto.request.UpdateAccountStatusRequest;
import com.astral.express.pccms.user.dto.response.AccountCredentialResponse;
import com.astral.express.pccms.user.dto.response.AccountResponse;
import com.astral.express.pccms.user.entity.UserStatus;
import com.astral.express.pccms.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/accounts")
@RequiredArgsConstructor
public class AdminAccountController {
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public ApiResponse<PageResponse<AccountResponse>> searchAccounts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) UserStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(userService.searchAccounts(keyword, role, status, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public ApiResponse<AccountCredentialResponse> createAccount(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.created(userService.createAccount(request));
    }

    @PutMapping("/{accountId}")
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public ApiResponse<AccountResponse> updateAccount(
            @PathVariable UUID accountId,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        return ApiResponse.success(userService.adminUpdateUser(accountId, request));
    }

    @PatchMapping("/{accountId}/status")
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public ApiResponse<AccountResponse> updateStatus(
            @PathVariable UUID accountId,
            @Valid @RequestBody UpdateAccountStatusRequest request) {
        return ApiResponse.success(userService.updateAccountStatus(accountId, request.statusCode()));
    }

    @PatchMapping("/{accountId}/role")
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public ApiResponse<AccountResponse> assignRole(
            @PathVariable UUID accountId,
            @Valid @RequestBody AssignAccountRoleRequest request) {
        return ApiResponse.success(userService.assignAccountRole(accountId, request.roleCode()));
    }

    @PostMapping("/{accountId}/password/reset")
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public ApiResponse<AccountCredentialResponse> resetPassword(@PathVariable UUID accountId) {
        return ApiResponse.success(userService.resetAccountPassword(accountId));
    }
}
