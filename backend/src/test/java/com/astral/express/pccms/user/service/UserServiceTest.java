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
import org.junit.jupiter.api.Test;
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
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.*;

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

    @Test
    void should_CreateAccount_Success() {
        CreateUserRequest createReq = new CreateUserRequest("Test User", "test@test.com", "CUSTOMER", null);
        com.astral.express.pccms.user.entity.Roles role = new com.astral.express.pccms.user.entity.Roles();
        role.setIsActive(true);
        role.setCode("CUSTOMER");
        
        Users mockUser = new Users();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("test@test.com");
        
        given(userRepository.existsByEmail("test@test.com")).willReturn(false);
        given(roleRepository.findByCodeIgnoreCase("CUSTOMER")).willReturn(Optional.of(role));
        given(userRepository.save(any(Users.class))).willReturn(mockUser);
        given(passwordEncoder.encode(anyString())).willReturn("hash");
        
        var result = userService.createAccount(createReq);
        assertThat(result).isNotNull();
        verify(emailService).sendAccountCreatedEmail(anyString(), anyString());
    }

    @Test
    void should_ThrowException_when_CreateAccount_EmailExists() {
        CreateUserRequest createReq = new CreateUserRequest("Test User", "test@test.com", "CUSTOMER", null);
        given(userRepository.existsByEmail("test@test.com")).willReturn(true);
        
        assertThatThrownBy(() -> userService.createAccount(createReq))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ACC_001_EMAIL_EXISTS);
    }

    @Test
    void should_ThrowException_when_AdminUpdateUser_EmailExists() {
        UUID userId = UUID.randomUUID();
        Users user = new Users();
        user.setId(userId);
        user.setEmail("old@test.com");
        
        AdminUpdateUserRequest updateReq = new AdminUpdateUserRequest("Name", "new@test.com", "0123456789", null, null);
        
        given(userRepository.findByIdWithRoleAndPermissions(userId)).willReturn(Optional.of(user));
        given(userRepository.existsByEmail("new@test.com")).willReturn(true);
        
        assertThatThrownBy(() -> userService.adminUpdateUser(userId, updateReq))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ACC_001_EMAIL_EXISTS);
    }

    @Test
    void should_UpdateUserStatus_AndRevokeTokens_when_Locked() {
        UUID userId = UUID.randomUUID();
        Users user = new Users();
        user.setId(userId);
        user.setEmail("test@test.com");
        
        AdminUpdateUserRequest updateReq = new AdminUpdateUserRequest("Name", null, null, null, UserStatus.LOCKED);
        
        given(userRepository.findByIdWithRoleAndPermissions(userId)).willReturn(Optional.of(user));
        given(userRepository.save(any(Users.class))).willReturn(user);
        
        userService.adminUpdateUser(userId, updateReq);
        
        verify(refreshTokenRepository).revokeAllUserTokens(userId);
    }

    @Test
    void should_ThrowException_when_FindActiveAccount_Deleted() {
        UUID accountId = UUID.randomUUID();
        Users user = new Users();
        user.setId(accountId);
        user.setDeletedAt(java.time.OffsetDateTime.now());
        
        given(userRepository.findByIdWithRoleAndPermissions(accountId)).willReturn(Optional.of(user));
        
        assertThatThrownBy(() -> userService.updateAccountStatus(accountId, UserStatus.ACTIVE))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ACC_002_USER_NOT_FOUND);
    }

    @Test
    void should_ThrowException_when_ResolveActiveRole_NotActive() {
        UUID accountId = UUID.randomUUID();
        Users user = new Users();
        user.setId(accountId);
        
        com.astral.express.pccms.user.entity.Roles role = new com.astral.express.pccms.user.entity.Roles();
        role.setIsActive(false);
        
        given(userRepository.findByIdWithRoleAndPermissions(accountId)).willReturn(Optional.of(user));
        given(roleRepository.findByCodeIgnoreCase("CUSTOMER")).willReturn(Optional.of(role));
        
        assertThatThrownBy(() -> userService.assignAccountRole(accountId, "CUSTOMER"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void should_SearchAccounts_WithFilters() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<Users> page = new org.springframework.data.domain.PageImpl<>(List.of(new Users()));
        
        given(userRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable))).willReturn(page);
        
        var result = userService.searchAccounts("keyword", "ROLE", UserStatus.ACTIVE, pageable);
        
        assertThat(result).isNotNull();
    }

    @Test
    void should_SearchAccounts_WithNoFilters() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        org.springframework.data.domain.Page<Users> page = new org.springframework.data.domain.PageImpl<>(List.of(new Users()));
        
        given(userRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable))).willReturn(page);
        
        var result = userService.searchAccounts(null, null, null, pageable);
        
        assertThat(result).isNotNull();
    }

    @Test
    void should_ResetAccountPassword() {
        UUID accountId = UUID.randomUUID();
        Users user = new Users();
        user.setId(accountId);
        user.setEmail("test@test.com");
        
        given(userRepository.findByIdWithRoleAndPermissions(accountId)).willReturn(Optional.of(user));
        given(passwordEncoder.encode(anyString())).willReturn("hash");
        given(userRepository.save(any(Users.class))).willReturn(user);
        
        var result = userService.resetAccountPassword(accountId);
        
        assertThat(result).isNotNull();
        verify(refreshTokenRepository).revokeAllUserTokens(accountId);
        verify(emailService).sendTemporaryPasswordEmail(eq("test@test.com"), anyString());
    }

    @Mock
    private com.astral.express.pccms.user.repository.StaffProfileRepository staffProfileRepository;

    @Test
    void should_CreateUser_WithStaffProfile_WhenRoleIsVeterinarian() {
        CreateUserRequest createReq = new CreateUserRequest("Vet User", "vet@test.com", "VETERINARIAN", null);
        com.astral.express.pccms.user.entity.Roles role = new com.astral.express.pccms.user.entity.Roles();
        role.setIsActive(true);
        role.setCode("VETERINARIAN");
        role.setName("Veterinarian");
        
        Users mockUser = new Users();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("vet@test.com");
        mockUser.setRole(role);
        
        given(userRepository.existsByEmail("vet@test.com")).willReturn(false);
        given(roleRepository.findByCodeIgnoreCase("VETERINARIAN")).willReturn(Optional.of(role));
        given(userRepository.save(any(Users.class))).willReturn(mockUser);
        given(passwordEncoder.encode(anyString())).willReturn("hash");
        
        userService.createUser(createReq);
        
        verify(staffProfileRepository).save(any(com.astral.express.pccms.user.entity.StaffProfile.class));
    }

    @Test
    void should_CreateUser_WithStaffProfile_WhenRoleIsStaff() {
        CreateUserRequest createReq = new CreateUserRequest("Staff User", "staff@test.com", "STAFF", null);
        com.astral.express.pccms.user.entity.Roles role = new com.astral.express.pccms.user.entity.Roles();
        role.setIsActive(true);
        role.setCode("STAFF");
        role.setName("Staff");
        
        Users mockUser = new Users();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("staff@test.com");
        mockUser.setRole(role);
        
        given(userRepository.existsByEmail("staff@test.com")).willReturn(false);
        given(roleRepository.findByCodeIgnoreCase("STAFF")).willReturn(Optional.of(role));
        given(userRepository.save(any(Users.class))).willReturn(mockUser);
        given(passwordEncoder.encode(anyString())).willReturn("hash");
        
        userService.createUser(createReq);
        
        verify(staffProfileRepository).save(any(com.astral.express.pccms.user.entity.StaffProfile.class));
    }

    @Test
    void should_AdminUpdateUser_UpdateAllFields_Successfully() {
        UUID userId = UUID.randomUUID();
        Users user = new Users();
        user.setId(userId);
        user.setEmail("old@test.com");
        
        com.astral.express.pccms.user.entity.Roles newRole = new com.astral.express.pccms.user.entity.Roles();
        newRole.setIsActive(true);
        newRole.setCode("NEW_ROLE");
        
        AdminUpdateUserRequest updateReq = new AdminUpdateUserRequest("New Name", "new@test.com", "0123456789", "NEW_ROLE", UserStatus.DISABLED);
        
        given(userRepository.findByIdWithRoleAndPermissions(userId)).willReturn(Optional.of(user));
        given(userRepository.existsByEmail("new@test.com")).willReturn(false);
        given(roleRepository.findByCodeIgnoreCase("NEW_ROLE")).willReturn(Optional.of(newRole));
        given(userRepository.save(any(Users.class))).willAnswer(i -> i.getArgument(0));
        
        var result = userService.adminUpdateUser(userId, updateReq);
        
        assertThat(result).isNotNull();
        assertThat(user.getFullName()).isEqualTo("New Name");
        assertThat(user.getEmail()).isEqualTo("new@test.com");
        assertThat(user.getPhone()).isEqualTo("0123456789");
        assertThat(user.getRole()).isEqualTo(newRole);
        assertThat(user.getStatusCode()).isEqualTo(UserStatus.DISABLED);
        
        verify(refreshTokenRepository).revokeAllUserTokens(userId);
    }
    
    @Test
    void should_AdminUpdateUser_UpdateStatusToActive() {
        UUID userId = UUID.randomUUID();
        Users user = new Users();
        user.setId(userId);
        user.setEmail("old@test.com");
        
        AdminUpdateUserRequest updateReq = new AdminUpdateUserRequest(null, null, null, null, UserStatus.ACTIVE);
        
        given(userRepository.findByIdWithRoleAndPermissions(userId)).willReturn(Optional.of(user));
        given(userRepository.save(any(Users.class))).willAnswer(i -> i.getArgument(0));
        
        userService.adminUpdateUser(userId, updateReq);
        
        assertThat(user.getStatusCode()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void should_UpdateAccountStatus_ToDisabled_AndRevokeTokens() {
        UUID accountId = UUID.randomUUID();
        Users user = new Users();
        user.setId(accountId);
        
        given(userRepository.findByIdWithRoleAndPermissions(accountId)).willReturn(Optional.of(user));
        given(userRepository.save(any(Users.class))).willReturn(user);
        
        userService.updateAccountStatus(accountId, UserStatus.DISABLED);
        
        verify(refreshTokenRepository).revokeAllUserTokens(accountId);
    }

    @Test
    void should_UpdateAccountStatus_ToActive() {
        UUID accountId = UUID.randomUUID();
        Users user = new Users();
        user.setId(accountId);
        
        given(userRepository.findByIdWithRoleAndPermissions(accountId)).willReturn(Optional.of(user));
        given(userRepository.save(any(Users.class))).willReturn(user);
        
        userService.updateAccountStatus(accountId, UserStatus.ACTIVE);
        
        // No tokens revoked
    }

}