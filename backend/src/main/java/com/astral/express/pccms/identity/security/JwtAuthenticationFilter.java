package com.astral.express.pccms.identity.security;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.service.CustomUserDetails;
import com.astral.express.pccms.identity.service.CustomUserDetailsService;
import com.astral.express.pccms.identity.service.TokenBlacklistService;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authorizationHeader.substring(7);

        String email;
        String userIdStr;

        try {
            email = jwtUtil.extractUsername(jwt);
            userIdStr = jwtUtil.extractUserId(jwt);
            if (userIdStr == null && email != null && isUuid(email)) {
                userIdStr = email;
                email = userRepository.findById(UUID.fromString(email))
                        .map(Users::getEmail)
                        .orElse(null);
            }
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }

        if (email != null
                && userIdStr != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            UUID userId = UUID.fromString(userIdStr);
            UserDetails userDetails =
                    this.userDetailsService.loadUserByUsername(email);

            if (isTokenAccepted(jwt, userDetails, userIdStr)) {
                String jti = jwtUtil.extractJti(jwt);
                boolean isBlacklisted = false;
                try { 
                    isBlacklisted = tokenBlacklistService.isBlacklisted(jti);
                } catch (org.springframework.data.redis.RedisConnectionFailureException ex) {
                    // Log warning, allow request if redis is down
                    System.err.println("Redis unavailable, proceeding without blacklist check: " + ex.getMessage());
                }
                
                if (isBlacklisted) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Token revoked\", \"message\": \"This token has been invalidated\"}");
                    return;
                }

                if (!userDetails.isEnabled()) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\": \"Account is locked\", \"message\": \"Your account has been locked\"}");
                    return;
                }

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                userDetails.getAuthorities()
                        );

                authenticationToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authenticationToken);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isTokenAccepted(String jwt, UserDetails userDetails, String userIdStr) {
        if (jwtUtil.validateToken(jwt, userDetails)) {
            return true;
        }
        if (userDetails instanceof CustomUserDetails custom
                && userIdStr != null
                && userIdStr.equals(custom.getId().toString())) {
            try {
                return !jwtUtil.isTokenExpired(jwt);
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    private static boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
