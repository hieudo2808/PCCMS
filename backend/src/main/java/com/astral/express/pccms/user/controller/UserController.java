package com.astral.express.pccms.user.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.identity.dto.request.OtpConfirmRequest;
import com.astral.express.pccms.identity.dto.request.OtpRequest;
import com.astral.express.pccms.identity.service.OtpService;
import com.astral.express.pccms.user.dto.request.AdminUpdateUserRequest;
import com.astral.express.pccms.user.dto.request.ChangePasswordRequest;
import com.astral.express.pccms.user.dto.request.CreateUserRequest;
import com.astral.express.pccms.user.dto.request.UserProfileUpdateRequest;
import com.astral.express.pccms.user.dto.response.AccountResponse;
import com.astral.express.pccms.user.dto.response.UserResponse;
import com.astral.express.pccms.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final OtpService otpService;

    @PostMapping
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(userService.createUser(request), "User created successfully. Credentials sent to email.");
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public ApiResponse<AccountResponse> adminUpdateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        return ApiResponse.success(userService.adminUpdateUser(userId, request), "User updated successfully");
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public ApiResponse<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ApiResponse.success(null, "User deleted successfully");
    }

    @PatchMapping("/{userId}/lock")
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public ApiResponse<Void> lockUser(@PathVariable UUID userId) {
        userService.adminLockUser(userId);
        return ApiResponse.success(null, "User locked successfully and sessions revoked");
    }

    @PatchMapping("/{userId}/disable")
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public ApiResponse<Void> disableUser(@PathVariable UUID userId) {
        userService.adminDisableUser(userId);
        return ApiResponse.success(null, "User disabled successfully and sessions revoked");
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public ApiResponse<UserResponse> getUser(@PathVariable UUID userId) {
        return ApiResponse.success(userService.getUser(userId));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ACCOUNT_MANAGE')")
    public ApiResponse<List<UserResponse>> getAllUsers() {
        return ApiResponse.success(userService.getAllUsers());
    }

    // ==================== USER SELF ENDPOINTS ====================

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserResponse> getMyProfile() {
        return ApiResponse.success(userService.getMyProfile());
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserResponse> updateMyProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        return ApiResponse.success(userService.updateMyProfile(request), "Profile updated successfully");
    }

    @PutMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ApiResponse.success(null, "Password changed successfully");
    }

    @PostMapping("/me/email-change/otp")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> requestEmailChangeOtp(@Valid @RequestBody OtpRequest request) {
        otpService.requestEmailChangeOtp(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/me/email-change/confirm")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserResponse> confirmEmailChange(@Valid @RequestBody OtpConfirmRequest request) {
        return ApiResponse.success(otpService.confirmEmailChange(request));
    }

    @PostMapping("/me/phone-change/otp")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> requestPhoneChangeOtp(@Valid @RequestBody OtpRequest request) {
        otpService.requestPhoneChangeOtp(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/me/phone-change/confirm")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<UserResponse> confirmPhoneChange(@Valid @RequestBody OtpConfirmRequest request) {
        return ApiResponse.success(otpService.confirmPhoneChange(request));
    }
}
