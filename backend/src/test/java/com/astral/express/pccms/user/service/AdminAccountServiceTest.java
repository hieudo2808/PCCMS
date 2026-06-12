package com.astral.express.pccms.user.service;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.repository.RefreshTokenRepository;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.notification.service.EmailService;
import com.astral.express.pccms.user.dto.response.AccountResponse;
import com.astral.express.pccms.user.entity.Roles;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.RecordComponent;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminAccountServiceTest {

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

    @ParameterizedTest(name = "[{1}] {3}")
    @CsvFileSource(resources = "/testcases/account-search.csv", numLinesToSkip = 1)
    void should_followAccountSearchCsvRules(
            String ruleId,
            String caseId,
            String useCase,
            String scenario,
            String precondition,
            String input,
            String expectedResult,
            String expectedErrorCode,
            String expectedMessage,
            String note) {
        assumeTrue(!"INVALID_STATUS_REJECTED".equals(scenario), "Invalid enum binding is covered by controller tests");

        AccountSearchInput searchInput = parseAccountSearchInput(input);
        PageRequest pageable = PageRequest.of(0, 20);

        if ("EXCEPTION".equals(expectedResult)) {
            assertThatThrownBy(() -> userService.searchAccounts(
                    searchInput.keyword(), searchInput.role(), searchInput.status(), pageable))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.valueOf(expectedErrorCode));
            verifyNoInteractions(userRepository);
            return;
        }

        Users user = switch (scenario) {
            case "NO_RESULT_SUCCESS", "SOFT_DELETED_USER_EXCLUDED" -> null;
            case "FILTER_BY_ROLE_SUCCESS" -> account("staff@pccms.vn", "Staff User", "STAFF", UserStatus.ACTIVE);
            case "FILTER_BY_STATUS_SUCCESS" -> account("active@pccms.vn", "Active User", "OWNER", UserStatus.ACTIVE);
            case "COMBINED_FILTER_SUCCESS" -> account("admin@pccms.vn", "Admin User", "ADMIN", UserStatus.ACTIVE);
            default -> account("admin@pccms.vn", "Admin User", "ADMIN", UserStatus.ACTIVE);
        };
        List<Users> users = user == null ? List.of() : List.of(user);

        given(userRepository.findAll(nullable(Specification.class), any(PageRequest.class)))
                .willReturn(new PageImpl<>(users, pageable, users.size()));

        PageResponse<AccountResponse> response = userService.searchAccounts(
                searchInput.keyword(), searchInput.role(), searchInput.status(), pageable);

        assertThat(response.data().content()).hasSize(users.size());
        if (!users.isEmpty()) {
            AccountResponse account = response.data().content().getFirst();
            assertThat(account.roleCode()).isEqualTo(user.getRole().getCode());
            assertThat(account.roleName()).isEqualTo(user.getRole().getName());
            assertThat(account.roles()).containsExactly(user.getRole().getCode());
        }
        if ("PASSWORD_HASH_NOT_RETURNED".equals(scenario)) {
            assertThat(AccountResponse.class.getRecordComponents())
                    .extracting(RecordComponent::getName)
                    .doesNotContain("passwordHash", "password_hash");
        }
    }

    @Test
    void should_useDynamicSpecification_when_SearchingByKeywordOnly() {
        PageRequest pageable = PageRequest.of(0, 20);
        given(userRepository.findAll(any(Specification.class), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(), pageable, 0));

        userService.searchAccounts("admin", null, null, pageable);

        verify(userRepository).findAll(any(Specification.class), eq(pageable));
    }

    @ParameterizedTest(name = "[{1}] {3}")
    @CsvFileSource(resources = "/testcases/account-status-update.csv", numLinesToSkip = 1)
    void should_followAccountStatusCsvRules(
            String ruleId,
            String caseId,
            String useCase,
            String scenario,
            String precondition,
            String input,
            String expectedResult,
            String expectedErrorCode,
            String expectedMessage,
            String note) {
        assumeTrue(!"INVALID_STATUS_REJECTED".equals(scenario), "Invalid enum binding is covered by controller tests");

        UUID userId = UUID.randomUUID();
        AccountStatusInput statusInput = parseAccountStatusInput(input);
        UserStatus status = UserStatus.valueOf(statusInput.statusCode());

        if ("ACCOUNT_NOT_FOUND".equals(scenario)) {
            given(userRepository.findByIdWithRoleAndPermissions(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateAccountStatus(userId, status))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.valueOf(expectedErrorCode));
            return;
        }

        Users user = account("staff@pccms.vn", "Staff User", "STAFF", UserStatus.ACTIVE);
        user.setId(userId);
        if ("SOFT_DELETED_ACCOUNT_REJECTED".equals(scenario)) {
            user.setDeletedAt(OffsetDateTime.now());
        }

        given(userRepository.findByIdWithRoleAndPermissions(userId)).willReturn(Optional.of(user));

        if ("EXCEPTION".equals(expectedResult)) {
            assertThatThrownBy(() -> userService.updateAccountStatus(userId, status))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.valueOf(expectedErrorCode));
            return;
        }

        given(userRepository.save(user)).willReturn(user);

        AccountResponse response = userService.updateAccountStatus(userId, status);

        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(user.getStatusCode()).isEqualTo(status);
        assertThat(user.getDeletedAt()).isNull();
        if (status == UserStatus.LOCKED || status == UserStatus.DISABLED) {
            verify(refreshTokenRepository).revokeAllUserTokens(userId);
        } else {
            verifyNoInteractions(refreshTokenRepository);
        }
    }

    @ParameterizedTest(name = "[{1}] {3}")
    @CsvFileSource(resources = "/testcases/account-role-assignment.csv", numLinesToSkip = 1)
    void should_followAccountRoleCsvRules(
            String ruleId,
            String caseId,
            String useCase,
            String scenario,
            String precondition,
            String input,
            String expectedResult,
            String expectedErrorCode,
            String expectedMessage,
            String note) {
        UUID userId = UUID.randomUUID();
        AccountRoleInput roleInput = parseAccountRoleInput(input);

        if ("USER_NOT_FOUND".equals(scenario)) {
            given(userRepository.findByIdWithRoleAndPermissions(userId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.assignAccountRole(userId, roleInput.roleCode()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.valueOf(expectedErrorCode));
            return;
        }

        Users user = account("staff@pccms.vn", "Staff User", "STAFF", UserStatus.ACTIVE);
        user.setId(userId);
        if ("SOFT_DELETED_USER_REJECTED".equals(scenario)) {
            user.setDeletedAt(OffsetDateTime.now());
        }

        given(userRepository.findByIdWithRoleAndPermissions(userId)).willReturn(Optional.of(user));

        if ("SOFT_DELETED_USER_REJECTED".equals(scenario)) {
            assertThatThrownBy(() -> userService.assignAccountRole(userId, roleInput.roleCode()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.valueOf(expectedErrorCode));
            return;
        }

        if ("ROLE_NOT_FOUND".equals(scenario) || "DOES_NOT_CREATE_ROLE".equals(scenario)) {
            given(roleRepository.findByCodeIgnoreCase(roleInput.roleCode())).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.assignAccountRole(userId, roleInput.roleCode()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.valueOf(expectedErrorCode));
            verify(roleRepository).findByCodeIgnoreCase(roleInput.roleCode());
            verifyNoMoreInteractions(roleRepository);
            return;
        }

        boolean activeRole = !"INACTIVE_ROLE_REJECTED".equals(scenario);
        Roles role = Roles.builder()
                .id(UUID.randomUUID())
                .code(roleInput.roleCode())
                .name(roleInput.roleCode() + " role")
                .isActive(activeRole)
                .build();
        given(roleRepository.findByCodeIgnoreCase(roleInput.roleCode())).willReturn(Optional.of(role));

        if ("EXCEPTION".equals(expectedResult)) {
            assertThatThrownBy(() -> userService.assignAccountRole(userId, roleInput.roleCode()))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.valueOf(expectedErrorCode));
            return;
        }

        given(userRepository.save(user)).willReturn(user);

        AccountResponse response = userService.assignAccountRole(userId, roleInput.roleCode());

        assertThat(user.getRole()).isSameAs(role);
        assertThat(response.roleCode()).isEqualTo(roleInput.roleCode());
        assertThat(response.roleName()).isEqualTo(role.getName());
        assertThat(response.roles()).containsExactly(roleInput.roleCode());
    }

    private Users account(String email, String fullName, String roleCode, UserStatus status) {
        Roles role = Roles.builder()
                .id(UUID.randomUUID())
                .code(roleCode)
                .name(roleCode + " role")
                .isActive(true)
                .build();
        Users user = Users.builder()
                .id(UUID.randomUUID())
                .email(email)
                .phone("0901234567")
                .passwordHash("secret")
                .fullName(fullName)
                .role(role)
                .statusCode(status)
                .build();
        user.setCreatedAt(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        user.setUpdatedAt(OffsetDateTime.parse("2026-01-02T00:00:00Z"));
        return user;
    }

    private AccountSearchInput parseAccountSearchInput(String input) {
        return new AccountSearchInput(
                value(input, "keyword"),
                value(input, "role"),
                enumValue(value(input, "status")));
    }

    private AccountStatusInput parseAccountStatusInput(String input) {
        return new AccountStatusInput(value(input, "statusCode"));
    }

    private AccountRoleInput parseAccountRoleInput(String input) {
        return new AccountRoleInput(value(input, "roleCode"));
    }

    private UserStatus enumValue(String value) {
        return value == null ? null : UserStatus.valueOf(value);
    }

    private String value(String input, String key) {
        for (String part : input.split(";")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length == 2 && pair[0].trim().equals(key)) {
                return normalize(pair[1]);
            }
        }
        return null;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record AccountSearchInput(String keyword, String role, UserStatus status) {
    }

    private record AccountStatusInput(String statusCode) {
    }

    private record AccountRoleInput(String roleCode) {
    }
}
