package com.astral.express.pccms.identity.controller;

import java.time.Duration;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.dto.request.LoginRequest;
import com.astral.express.pccms.identity.dto.request.OtpRequest;
import com.astral.express.pccms.identity.dto.request.PasswordResetConfirmRequest;
import com.astral.express.pccms.identity.dto.request.RegisterRequest;
import com.astral.express.pccms.identity.dto.response.AuthResponse;
import com.astral.express.pccms.identity.service.AuthService;
import com.astral.express.pccms.identity.service.OtpService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final OtpService otpService;
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final Duration REFRESH_TOKEN_MAX_AGE = Duration.ofDays(7);

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.register(request);

        setRefreshTokenCookie(response, authResponse.getRefreshToken());
        authResponse.setRefreshToken(null);

        return ApiResponse.success(authResponse);
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        log.info("LOGIN_CONTROLLER_REACHED email={}", request.email());

        AuthResponse authResponse = authService.login(request);

        setRefreshTokenCookie(response, authResponse.getRefreshToken());
        authResponse.setRefreshToken(null);

        return ApiResponse.success(authResponse);
    }

    @PostMapping("/refresh")
    public ApiResponse<?> refreshToken(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null || refreshToken.isEmpty())
            throw new BusinessException(ErrorCode.ERR_IAM_001_INVALID_CREDENTIALS);

        try {
            AuthResponse authResponse = authService.refreshAccessToken(refreshToken);

            setRefreshTokenCookie(response, authResponse.getRefreshToken());
            authResponse.setRefreshToken(null);

            return ApiResponse.success(authResponse);
        } catch (Exception e) {
            clearRefreshTokenCookie(response);
            log.error("Token refresh failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.ERR_IAM_001_INVALID_CREDENTIALS);
        }
    }

    @PostMapping("/password-reset/otp")
    public ApiResponse<Void> requestPasswordResetOtp(@Valid @RequestBody OtpRequest request) {
        otpService.requestPasswordResetOtp(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/password-reset/confirm")
    public ApiResponse<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        otpService.confirmPasswordReset(request);
        return ApiResponse.success(null);
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {

        String accessToken = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        try {
            authService.logout(accessToken, refreshToken);
        } catch (Exception e) {
            log.atDebug().log(e.getMessage());
        }

        clearRefreshTokenCookie(response);
        return ApiResponse.success(null, "Logout success");
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(false) // Set to true in production with HTTPS
                .sameSite("Lax")
                .path("/")
                .maxAge(REFRESH_TOKEN_MAX_AGE)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
