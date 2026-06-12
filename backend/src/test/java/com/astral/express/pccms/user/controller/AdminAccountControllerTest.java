package com.astral.express.pccms.user.controller;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.user.dto.request.AssignAccountRoleRequest;
import com.astral.express.pccms.user.dto.request.CreateUserRequest;
import com.astral.express.pccms.user.dto.request.UpdateAccountStatusRequest;
import com.astral.express.pccms.user.dto.response.AccountCredentialResponse;
import com.astral.express.pccms.user.dto.response.AccountResponse;
import com.astral.express.pccms.user.dto.response.UserResponse;
import com.astral.express.pccms.user.entity.UserStatus;
import com.astral.express.pccms.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminAccountControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminAccountController adminAccountController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminAccountController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void searchAccounts_returnsPage() throws Exception {
        AccountResponse account = accountResponse(UserStatus.ACTIVE, "STAFF", "Nhan vien trung tam");
        given(userService.searchAccounts(eq("staff"), eq("STAFF"), eq(UserStatus.ACTIVE), any(Pageable.class)))
                .willReturn(PageResponse.of(new PageImpl<>(List.of(account))));

        mockMvc.perform(get("/v1/admin/accounts")
                        .param("keyword", "staff")
                        .param("role", "STAFF")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data.content[0].roleCode").value("STAFF"))
                .andExpect(jsonPath("$.data.data.content[0].roles[0]").value("STAFF"));
    }

    @Test
    void searchAccounts_returnsPage_whenCriteriaAreEmpty() throws Exception {
        AccountResponse account = accountResponse(UserStatus.ACTIVE, "OWNER", "Chu nuoi");
        given(userService.searchAccounts(eq(" "), eq(" "), eq(null), any(Pageable.class)))
                .willReturn(PageResponse.of(new PageImpl<>(List.of(account))));

        mockMvc.perform(get("/v1/admin/accounts")
                        .param("keyword", " ")
                        .param("role", " "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data.content[0].roleCode").value("OWNER"));
    }

    @Test
    void createAccount_returnsCreatedUser() throws Exception {
        CreateUserRequest request = new CreateUserRequest("Staff User", "staff@pccms.vn", "STAFF", "0901234567");
        AccountResponse accountResp = accountResponse(UserStatus.ACTIVE, "STAFF", "Nhan vien trung tam");
        AccountCredentialResponse response = new AccountCredentialResponse(accountResp, "temp123", true);
        given(userService.createAccount(request)).willReturn(response);

        mockMvc.perform(post("/v1/admin/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.account.roleCode").value("STAFF"));
    }

    @ParameterizedTest(name = "[{1}] {3}")
    @CsvFileSource(resources = "/testcases/account-search.csv", numLinesToSkip = 1)
    void searchAccounts_returnsValidationError_whenStatusIsInvalid(
            String ruleId,
            String caseId,
            String useCase,
            String scenario,
            String precondition,
            String input,
            String expectedResult,
            String expectedErrorCode,
            String expectedMessage,
            String note) throws Exception {
        if (!"INVALID_STATUS_REJECTED".equals(scenario)) {
            return;
        }

        mockMvc.perform(get("/v1/admin/accounts")
                        .param("status", "INACTIVE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(expectedErrorCode));

        verifyNoInteractions(userService);
    }

    @Test
    void updateAccountStatus_returnsUpdatedAccount() throws Exception {
        UUID accountId = UUID.randomUUID();
        AccountResponse account = accountResponse(UserStatus.LOCKED, "OWNER", "Chu nuoi");
        given(userService.updateAccountStatus(accountId, UserStatus.LOCKED)).willReturn(account);

        mockMvc.perform(patch("/v1/admin/accounts/{accountId}/status", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateAccountStatusRequest(UserStatus.LOCKED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.statusCode").value("LOCKED"));
    }

    @ParameterizedTest(name = "[{1}] {3}")
    @CsvFileSource(resources = "/testcases/account-status-update.csv", numLinesToSkip = 1)
    void updateAccountStatus_returnsValidationError_whenStatusIsInvalid(
            String ruleId,
            String caseId,
            String useCase,
            String scenario,
            String precondition,
            String input,
            String expectedResult,
            String expectedErrorCode,
            String expectedMessage,
            String note) throws Exception {
        if (!"INVALID_STATUS_REJECTED".equals(scenario)) {
            return;
        }

        mockMvc.perform(patch("/v1/admin/accounts/{accountId}/status", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statusCode\":\"BANNED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(expectedErrorCode));

        verifyNoInteractions(userService);
    }

    @Test
    void assignAccountRole_returnsUpdatedAccount() throws Exception {
        UUID accountId = UUID.randomUUID();
        AccountResponse account = accountResponse(UserStatus.ACTIVE, "STAFF", "Nhan vien trung tam");
        given(userService.assignAccountRole(accountId, "STAFF")).willReturn(account);

        mockMvc.perform(patch("/v1/admin/accounts/{accountId}/role", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignAccountRoleRequest("STAFF"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roleCode").value("STAFF"))
                .andExpect(jsonPath("$.data.roles[0]").value("STAFF"));
    }

    @ParameterizedTest(name = "[{1}] {3}")
    @CsvFileSource(resources = "/testcases/account-role-assignment.csv", numLinesToSkip = 1)
    void assignAccountRole_returnsRoleNotFound_whenRoleDoesNotExist(
            String ruleId,
            String caseId,
            String useCase,
            String scenario,
            String precondition,
            String input,
            String expectedResult,
            String expectedErrorCode,
            String expectedMessage,
            String note) throws Exception {
        if (!"ROLE_NOT_FOUND".equals(scenario) && !"DOES_NOT_CREATE_ROLE".equals(scenario)) {
            return;
        }
        UUID accountId = UUID.randomUUID();
        String roleCode = roleCode(input);
        willThrow(new BusinessException(ErrorCode.valueOf(expectedErrorCode)))
                .given(userService).assignAccountRole(accountId, roleCode);

        mockMvc.perform(patch("/v1/admin/accounts/{accountId}/role", accountId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleCode\":\"" + roleCode + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(expectedErrorCode));

        verify(userService).assignAccountRole(accountId, roleCode);
    }

    private String roleCode(String input) {
        for (String part : input.split(";")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length == 2 && pair[0].trim().equals("roleCode")) {
                return pair[1].trim();
            }
        }
        throw new IllegalArgumentException("roleCode is missing from CSV input");
    }

    private AccountResponse accountResponse(UserStatus status, String roleCode, String roleName) {
        return new AccountResponse(
                UUID.randomUUID(),
                "staff@pccms.vn",
                "0901234567",
                "Staff User",
                roleCode,
                roleName,
                List.of(roleCode),
                status,
                OffsetDateTime.parse("2026-01-01T00:00:00Z"),
                OffsetDateTime.parse("2026-01-02T00:00:00Z")
        );
    }
}
