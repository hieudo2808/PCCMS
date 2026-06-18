package com.astral.express.pccms.identity.controller;

import com.astral.express.pccms.common.exception.GlobalExceptionHandler;
import com.astral.express.pccms.identity.dto.request.LoginRequest;
import com.astral.express.pccms.identity.dto.request.OtpRequest;
import com.astral.express.pccms.identity.dto.request.PasswordResetConfirmRequest;
import com.astral.express.pccms.identity.dto.request.RegisterRequest;
import com.astral.express.pccms.identity.dto.response.AuthResponse;
import com.astral.express.pccms.identity.service.AuthService;
import com.astral.express.pccms.identity.service.OtpService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import jakarta.servlet.http.Cookie;
import org.mockito.BDDMockito;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private OtpService otpService;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void register_success() throws Exception {
        RegisterRequest request = new RegisterRequest("Test User", "test@gmail.com", "0901234567", "Password123!");
        AuthResponse response = new AuthResponse("access_token", "refresh_token", null);

        given(authService.register(any(RegisterRequest.class))).willReturn(response);

        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("access_token"))
                .andExpect(cookie().exists("refresh_token"));
    }

    @Test
    void register_requiresPhone() throws Exception {
        mockMvc.perform(post("/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"Test User","email":"test@gmail.com","password":"Password123!"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("ERR_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.phone").exists());
    }

    @Test
    void login_success() throws Exception {
        LoginRequest request = new LoginRequest("test@gmail.com", "Password123!");
        AuthResponse response = new AuthResponse("access_token", "refresh_token", null);

        given(authService.login(any(LoginRequest.class))).willReturn(response);

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("access_token"))
                .andExpect(cookie().exists("refresh_token"));
    }

    @Test
    void login_setsSecureRefreshCookie_WhenConfigured() throws Exception {
        ReflectionTestUtils.setField(authController, "refreshCookieSecure", true);
        LoginRequest request = new LoginRequest("test@gmail.com", "Password123!");
        AuthResponse response = new AuthResponse("access_token", "refresh_token", null);

        given(authService.login(any(LoginRequest.class))).willReturn(response);

        mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")));
    }

    @Test
    void logout_clearsSecureRefreshCookie_WhenConfigured() throws Exception {
        ReflectionTestUtils.setField(authController, "refreshCookieSecure", true);

        mockMvc.perform(post("/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Secure")));
    }

    @Test
    void refreshToken_success() throws Exception {
        AuthResponse response = new AuthResponse("access_token", "new_refresh_token", null);

        given(authService.refreshAccessToken("old_refresh_token")).willReturn(response);

        mockMvc.perform(post("/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", "old_refresh_token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("access_token"))
                .andExpect(cookie().exists("refresh_token"));
    }

    @Test
    void logout_success() throws Exception {
        mockMvc.perform(post("/v1/auth/logout")
                        .header("Authorization", "Bearer access_token")
                        .cookie(new Cookie("refresh_token", "refresh_token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(cookie().exists("refresh_token"));

        verify(authService).logout("access_token", "refresh_token");
    }

    @Test
    void requestPasswordResetOtp_success() throws Exception {
        OtpRequest request = new OtpRequest("test@gmail.com");

        mockMvc.perform(post("/v1/auth/password-reset/otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(otpService).requestPasswordResetOtp(any(OtpRequest.class));
    }

    @Test
    void confirmPasswordReset_success() throws Exception {
        PasswordResetConfirmRequest request = new PasswordResetConfirmRequest("test@gmail.com", "123456", "Password123!");

        mockMvc.perform(post("/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(otpService).confirmPasswordReset(any(PasswordResetConfirmRequest.class));
    }

    @Test
    void refreshToken_exception() throws Exception {
        given(authService.refreshAccessToken(any())).willThrow(new RuntimeException("invalid"));

        mockMvc.perform(post("/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", "invalid")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshToken_empty() throws Exception {
        mockMvc.perform(post("/v1/auth/refresh")
                        .cookie(new Cookie("refresh_token", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshToken_null() throws Exception {
        mockMvc.perform(post("/v1/auth/refresh"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_exception() throws Exception {
        BDDMockito.willThrow(new RuntimeException("error")).given(authService).logout(any(), any());

        mockMvc.perform(post("/v1/auth/logout")
                        .header("Authorization", "Bearer token")
                        .cookie(new Cookie("refresh_token", "invalid")))
                .andExpect(status().isOk());
    }

    @Test
    void logout_no_header() throws Exception {
        mockMvc.perform(post("/v1/auth/logout"))
                .andExpect(status().isOk());
    }

    @Test
    void logout_invalid_header() throws Exception {
        mockMvc.perform(post("/v1/auth/logout")
                        .header("Authorization", "Basic token"))
                .andExpect(status().isOk());
    }

}
