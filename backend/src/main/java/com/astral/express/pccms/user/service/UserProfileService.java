package com.astral.express.pccms.user.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.user.dto.request.ChangePasswordRequest;
import com.astral.express.pccms.user.dto.request.UserProfileUpdateRequest;
import com.astral.express.pccms.user.dto.response.UserResponse;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.mapper.UserMapper;
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
public class UserProfileService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextService securityContextService;

    @PreAuthorize("isAuthenticated()")
    public UserResponse getMyProfile() {
        UUID userId = securityContextService.getCurrentUserId();
        Users user = userRepository.findByIdWithRoleAndPermissions(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
        return userMapper.toUserResponse(user);
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public UserResponse updateMyProfile(UserProfileUpdateRequest request) {
        UUID userId = securityContextService.getCurrentUserId();
        Users user = userRepository.findByIdWithRoleAndPermissions(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));

        userMapper.updateProfile(request, user);

        log.info("User updated profile: {}", user.getEmail());
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    @PreAuthorize("isAuthenticated()")
    public void changePassword(ChangePasswordRequest request) {
        UUID userId = securityContextService.getCurrentUserId();
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
