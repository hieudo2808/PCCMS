package com.astral.express.pccms.user.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.repository.RefreshTokenRepository;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.notification.service.EmailService;
import com.astral.express.pccms.user.dto.request.AdminUpdateUserRequest;
import com.astral.express.pccms.user.dto.request.ChangePasswordRequest;
import com.astral.express.pccms.user.dto.request.CreateUserRequest;
import com.astral.express.pccms.user.dto.request.UserProfileUpdateRequest;
import com.astral.express.pccms.user.dto.response.UserResponse;
import com.astral.express.pccms.user.entity.UserStatus;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.mapper.UserMapper;
import com.astral.express.pccms.user.repository.RoleRepository;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private SecurityContextService SecurityContextService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserService userService;

    @ParameterizedTest(name = "[{0}] {1}: {6}")
    @CsvFileSource(resources = "/testcases/user-service-testcases.csv", numLinesToSkip = 1)
    void executeUserServiceTests(String ruleId, String caseId, String action, String mockState, String expectedResult, ErrorCode expectedError, String note) {
        UUID userId = UUID.randomUUID();
        Users mockUser = new Users();
        mockUser.setId(userId);
        mockUser.setStatusCode(UserStatus.ACTIVE);
        mockUser.setPasswordHash("oldHash");
        
        CreateUserRequest createReq = new CreateUserRequest("Test User", "test@test.com", "CUSTOMER", null);
        AdminUpdateUserRequest adminUpdateReq = new AdminUpdateUserRequest("New Name", null, null, null, UserStatus.ACTIVE);
        UserProfileUpdateRequest profileUpdateReq = new UserProfileUpdateRequest("New Name", "123456789");
        ChangePasswordRequest passwordReq = new ChangePasswordRequest("oldPass", "newPass");

        // GIVEN
        switch (action) {
            case "LOCK_USER":
            case "DISABLE_USER":
            case "DELETE_USER":
                if ("VALID".equals(mockState)) {
                    given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
                    given(userRepository.save(any(Users.class))).willAnswer(inv -> inv.getArgument(0));
                } else if ("USER_NOT_FOUND".equals(mockState)) {
                    given(userRepository.findById(userId)).willReturn(Optional.empty());
                }
                break;
            case "ADMIN_UPDATE_USER":
            case "GET_USER":
                if ("VALID".equals(mockState)) {
                    given(userRepository.findByIdWithRoleAndPermissions(userId)).willReturn(Optional.of(mockUser));
                    if ("ADMIN_UPDATE_USER".equals(action)) {
                        given(userRepository.save(mockUser)).willReturn(mockUser);
                        given(userMapper.toUserResponse(mockUser)).willReturn(new UserResponse(userId, "New Name", "email", "phone", "CUSTOMER", null, UserStatus.ACTIVE));
                    } else {
                        given(userMapper.toUserResponse(mockUser)).willReturn(new UserResponse(userId, "Name", "email", "phone", "CUSTOMER", null, UserStatus.ACTIVE));
                    }
                } else if ("USER_NOT_FOUND".equals(mockState)) {
                    given(userRepository.findByIdWithRoleAndPermissions(userId)).willReturn(Optional.empty());
                }
                break;
            case "CREATE_USER":
                if ("VALID".equals(mockState)) {
                    given(userRepository.existsByEmail(createReq.email())).willReturn(false);
                    given(roleRepository.findByCodeIgnoreCase("CUSTOMER")).willReturn(Optional.of(new com.astral.express.pccms.user.entity.Roles()));
                    given(passwordEncoder.encode(anyString())).willReturn("hash");
                    given(userRepository.save(any(Users.class))).willReturn(mockUser);
                    given(userMapper.toUserResponse(mockUser)).willReturn(new UserResponse(userId, "Name", "email", "phone", "CUSTOMER", null, UserStatus.ACTIVE));
                } else if ("EMAIL_EXISTS".equals(mockState)) {
                    given(userRepository.existsByEmail(createReq.email())).willReturn(true);
                } else if ("ROLE_NOT_FOUND".equals(mockState)) {
                    given(userRepository.existsByEmail(createReq.email())).willReturn(false);
                    given(roleRepository.findByCodeIgnoreCase("CUSTOMER")).willReturn(Optional.empty());
                }
                break;
            case "GET_ALL_USERS":
                given(userRepository.findAll()).willReturn(List.of(mockUser));
                break;
            case "GET_MY_PROFILE":
            case "UPDATE_MY_PROFILE":
                given(SecurityContextService.getCurrentUserId()).willReturn(userId);
                if ("USER_NOT_FOUND".equals(mockState)) {
                    given(userRepository.findByIdWithRoleAndPermissions(userId)).willReturn(Optional.empty());
                } else {
                    given(userRepository.findByIdWithRoleAndPermissions(userId)).willReturn(Optional.of(mockUser));
                    if ("UPDATE_MY_PROFILE".equals(action)) {
                        given(userRepository.save(mockUser)).willReturn(mockUser);
                        given(userMapper.toUserResponse(mockUser)).willReturn(new UserResponse(userId, "New Name", "email", "phone", "CUSTOMER", null, UserStatus.ACTIVE));
                    } else {
                        given(userMapper.toUserResponse(mockUser)).willReturn(new UserResponse(userId, "Name", "email", "phone", "CUSTOMER", null, UserStatus.ACTIVE));
                    }
                }
                break;
            case "CHANGE_PASSWORD":
                given(SecurityContextService.getCurrentUserId()).willReturn(userId);
                if ("USER_NOT_FOUND".equals(mockState)) {
                    given(userRepository.findById(userId)).willReturn(Optional.empty());
                } else {
                    given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
                    if ("WRONG_PASSWORD".equals(mockState)) {
                        given(passwordEncoder.matches("oldPass", "oldHash")).willReturn(false);
                    } else {
                        given(passwordEncoder.matches("oldPass", "oldHash")).willReturn(true);
                        given(passwordEncoder.encode("newPass")).willReturn("newHash");
                        given(userRepository.save(mockUser)).willReturn(mockUser);
                    }
                }
                break;
        }

        // WHEN & THEN
        if ("EXCEPTION".equals(expectedResult)) {
            assertThatThrownBy(() -> {
                switch (action) {
                    case "LOCK_USER": userService.adminLockUser(userId); break;
                    case "DISABLE_USER": userService.adminDisableUser(userId); break;
                    case "ADMIN_UPDATE_USER": userService.adminUpdateUser(userId, adminUpdateReq); break;
                    case "DELETE_USER": userService.deleteUser(userId); break;
                    case "GET_USER": userService.getUser(userId); break;
                    case "CREATE_USER": userService.createUser(createReq); break;
                    case "GET_MY_PROFILE": userService.getMyProfile(); break;
                    case "UPDATE_MY_PROFILE": userService.updateMyProfile(profileUpdateReq); break;
                    case "CHANGE_PASSWORD": userService.changePassword(passwordReq); break;
                }
            }).isInstanceOf(BusinessException.class)
              .hasFieldOrPropertyWithValue("errorCode", expectedError);
        } else {
            switch (action) {
                case "LOCK_USER":
                    userService.adminLockUser(userId);
                    assertThat(mockUser.getStatusCode()).isEqualTo(UserStatus.LOCKED);
                    break;
                case "DISABLE_USER":
                    userService.adminDisableUser(userId);
                    assertThat(mockUser.getStatusCode()).isEqualTo(UserStatus.DISABLED);
                    break;
                case "ADMIN_UPDATE_USER":
                    userService.adminUpdateUser(userId, adminUpdateReq);
                    break;
                case "DELETE_USER":
                    userService.deleteUser(userId);
                    break;
                case "GET_USER":
                    userService.getUser(userId);
                    break;
                case "CREATE_USER":
                    userService.createUser(createReq);
                    break;
                case "GET_ALL_USERS":
                    userService.getAllUsers();
                    break;
                case "GET_MY_PROFILE":
                    userService.getMyProfile();
                    break;
                case "UPDATE_MY_PROFILE":
                    userService.updateMyProfile(profileUpdateReq);
                    break;
                case "CHANGE_PASSWORD":
                    userService.changePassword(passwordReq);
                    assertThat(mockUser.getPasswordHash()).isEqualTo("newHash");
                    break;
            }
        }
    }
}
