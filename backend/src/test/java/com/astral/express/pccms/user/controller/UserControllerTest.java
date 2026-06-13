package com.astral.express.pccms.user.controller;

import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.identity.dto.request.OtpConfirmRequest;
import com.astral.express.pccms.identity.dto.request.OtpRequest;
import com.astral.express.pccms.identity.service.OtpService;
import com.astral.express.pccms.user.dto.request.AdminUpdateUserRequest;
import com.astral.express.pccms.user.dto.request.ChangePasswordRequest;
import com.astral.express.pccms.user.dto.request.CreateUserRequest;
import com.astral.express.pccms.user.dto.request.UserProfileUpdateRequest;
import com.astral.express.pccms.user.dto.response.AccountResponse;
import com.astral.express.pccms.user.dto.response.UserResponse;
import com.astral.express.pccms.user.entity.UserStatus;
import com.astral.express.pccms.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private OtpService otpService;

    @InjectMocks
    private UserController controller;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private UserResponse mockUserResponse() {
        return new UserResponse(
                UUID.randomUUID(),
                "Test User",
                "test@gmail.com",
                "0901234567",
                "CUSTOMER",
                OffsetDateTime.now(),
                UserStatus.ACTIVE
        );
    }

    private AccountResponse mockAccountResponse() {
        return new AccountResponse(
                UUID.randomUUID(),
                "test@gmail.com",
                "0901234567",
                "Test User",
                "CUSTOMER",
                "Khach hang",
                List.of("CUSTOMER"),
                UserStatus.ACTIVE,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    @Test
    void createUser_success() throws Exception {
        CreateUserRequest request = new CreateUserRequest("Test User", "test@gmail.com", "STAFF", "0901234567");
        given(userService.createUser(any())).willReturn(mockUserResponse());

        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void adminUpdateUser_success() throws Exception {
        UUID id = UUID.randomUUID();
        AdminUpdateUserRequest request = new AdminUpdateUserRequest("New Name", "new@gmail.com", "0901234567", "STAFF", UserStatus.ACTIVE);
        given(userService.adminUpdateUser(any(), any())).willReturn(mockAccountResponse());

        mockMvc.perform(put("/v1/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void deleteUser_success() throws Exception {
        mockMvc.perform(delete("/v1/users/{id}", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void lockUser_success() throws Exception {
        mockMvc.perform(patch("/v1/users/{id}/lock", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void disableUser_success() throws Exception {
        mockMvc.perform(patch("/v1/users/{id}/disable", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getUser_success() throws Exception {
        given(userService.getUser(any())).willReturn(mockUserResponse());
        mockMvc.perform(get("/v1/users/{id}", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getAllUsers_success() throws Exception {
        given(userService.getAllUsers()).willReturn(List.of(mockUserResponse()));
        mockMvc.perform(get("/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getMyProfile_success() throws Exception {
        given(userService.getMyProfile()).willReturn(mockUserResponse());
        mockMvc.perform(get("/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateMyProfile_success() throws Exception {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest("New Name", "Avatar");
        given(userService.updateMyProfile(any())).willReturn(mockUserResponse());

        mockMvc.perform(put("/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void changePassword_success() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("oldpass", "Password123!");

        mockMvc.perform(put("/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void requestEmailChangeOtp_success() throws Exception {
        OtpRequest request = new OtpRequest("new@gmail.com");

        mockMvc.perform(post("/v1/users/me/email-change/otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void confirmEmailChange_success() throws Exception {
        OtpConfirmRequest request = new OtpConfirmRequest("new@gmail.com", "123456");
        given(otpService.confirmEmailChange(any())).willReturn(mockUserResponse());

        mockMvc.perform(post("/v1/users/me/email-change/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void requestPhoneChangeOtp_success() throws Exception {
        OtpRequest request = new OtpRequest("0901234567");

        mockMvc.perform(post("/v1/users/me/phone-change/otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void confirmPhoneChange_success() throws Exception {
        OtpConfirmRequest request = new OtpConfirmRequest("0901234567", "123456");
        given(otpService.confirmPhoneChange(any())).willReturn(mockUserResponse());

        mockMvc.perform(post("/v1/users/me/phone-change/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}