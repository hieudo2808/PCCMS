package com.astral.express.pccms.user.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.user.dto.request.AdminUpdateUserRequest;
import com.astral.express.pccms.user.dto.request.ChangePasswordRequest;
import com.astral.express.pccms.user.dto.request.CreateUserRequest;
import com.astral.express.pccms.user.dto.request.UserProfileUpdateRequest;
import com.astral.express.pccms.user.dto.response.UserResponse;
import com.astral.express.pccms.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping
    public ApiResponse<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(userService.createUser(request), "User created successfully. Credentials sent to email.");
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserResponse> adminUpdateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        return ApiResponse.success(userService.adminUpdateUser(userId, request), "User updated successfully");
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ApiResponse.success(null, "User deleted successfully");
    }

    @PatchMapping("/{userId}/lock")
    public ApiResponse<Void> lockUser(@PathVariable UUID userId) {
        userService.adminLockUser(userId);
        return ApiResponse.success(null, "User locked successfully and sessions revoked");
    }

    @PatchMapping("/{userId}/disable")
    public ApiResponse<Void> disableUser(@PathVariable UUID userId) {
        userService.adminDisableUser(userId);
        return ApiResponse.success(null, "User disabled successfully and sessions revoked");
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUser(@PathVariable UUID userId) {
        return ApiResponse.success(userService.getUser(userId));
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> getAllUsers() {
        return ApiResponse.success(userService.getAllUsers());
    }

    // ==================== USER SELF ENDPOINTS ====================

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyProfile() {
        return ApiResponse.success(userService.getMyProfile());
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateMyProfile(@Valid @RequestBody UserProfileUpdateRequest request) {
        return ApiResponse.success(userService.updateMyProfile(request), "Profile updated successfully");
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ApiResponse.success(null, "Password changed successfully");
    }
}
