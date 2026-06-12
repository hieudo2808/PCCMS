package com.astral.express.pccms.identity.security;

import com.astral.express.pccms.user.entity.UserStatus;

import com.astral.express.pccms.identity.service.CustomUserDetails;
import com.astral.express.pccms.identity.service.CustomUserDetailsService;
import com.astral.express.pccms.identity.service.TokenBlacklistService;
import com.astral.express.pccms.user.entity.Permission;
import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class JwtAuthenticationFilterTest {

    private static final String BASE64URL_TEST_SECRET =
            "ysUgFpYjVfEDzZ_z5Vb0OxAKRG7Vfsv7m2T4s0NNor_I855Q3T2ypzUbWqxjkwWMigxSfCPCqehH6N2UGqgaEQ";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_SetSecurityContext_when_AccessTokenSubjectIsUserId() throws Exception {
        Users user = activeAdminUser();
        JwtUtil jwtUtil = configuredJwtUtil();
        String token = jwtUtil.generateToken(user);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);
        UserRepository userRepository = mock(UserRepository.class);
        given(userDetailsService.loadUserByUsername(user.getEmail())).willReturn(userDetails);
        given(tokenBlacklistService.isBlacklisted(jwtUtil.extractJti(token))).willReturn(false);

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtUtil, userDetailsService, tokenBlacklistService, userRepository);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/accounts");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(user.getId());
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .contains("ACCOUNT_MANAGE");
    }

    @Test
    void should_SetSecurityContext_when_TokenBlacklistBackendIsUnavailable() throws Exception {
        Users user = activeAdminUser();
        JwtUtil jwtUtil = configuredJwtUtil();
        String token = jwtUtil.generateToken(user);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        TokenBlacklistService tokenBlacklistService = mock(TokenBlacklistService.class);
        UserRepository userRepository = mock(UserRepository.class);
        given(userDetailsService.loadUserByUsername(user.getEmail())).willReturn(userDetails);
        given(tokenBlacklistService.isBlacklisted(jwtUtil.extractJti(token)))
                .willThrow(new RedisConnectionFailureException("redis unavailable"));

        JwtAuthenticationFilter filter =
                new JwtAuthenticationFilter(jwtUtil, userDetailsService, tokenBlacklistService, userRepository);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/accounts");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        filter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isEqualTo(user.getId());
    }

    private JwtUtil configuredJwtUtil() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", BASE64URL_TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 600_000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 604_800_000L);
        return jwtUtil;
    }

    private Users activeAdminUser() {
        Permission permission = Permission.builder()
                .id(UUID.randomUUID())
                .code("ACCOUNT_MANAGE")
                .name("Account management")
                .build();
        Roles role = Roles.builder()
                .id(UUID.randomUUID())
                .code("ADMIN")
                .name("Administrator")
                .permissions(Set.of(permission))
                .build();
        return Users.builder()
                .id(UUID.randomUUID())
                .email("admin@pccms.vn")
                .passwordHash("$2a$12$test")
                .fullName("System Admin")
                .role(role)
                .statusCode(UserStatus.ACTIVE)
                .build();
    }
}
