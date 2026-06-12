package com.astral.express.pccms.identity.security;

import com.astral.express.pccms.common.exception.ErrorResponse;
import com.astral.express.pccms.common.exception.ErrorCode;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    private final StringRedisTemplate redisTemplate;
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final int AUTH_REQUESTS_PER_MINUTE = 20;
    private static final int GENERAL_REQUESTS_PER_MINUTE = 200;
    private static final long WINDOW_SECONDS = 60;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String clientIp = getClientIP(request);
        String path = request.getRequestURI();

        boolean isAuthEndpoint = path.contains("/api/auth") || path.contains("/auth");
        String bucketKey = RATE_LIMIT_PREFIX + clientIp + (isAuthEndpoint ? ":auth" : ":general");
        int limit = isAuthEndpoint ? AUTH_REQUESTS_PER_MINUTE : GENERAL_REQUESTS_PER_MINUTE;

        Long currentCount;
        try {
            currentCount = redisTemplate.opsForValue().increment(bucketKey);

            if (currentCount != null && currentCount == 1) {
                redisTemplate.expire(bucketKey, WINDOW_SECONDS, TimeUnit.SECONDS);
            }
        } catch (DataAccessException ex) {
            log.warn("Rate limit backend unavailable; allowing request for IP: {} on path: {}", clientIp, path);
            filterChain.doFilter(request, response);
            return;
        }

        if (currentCount != null && currentCount > limit) {
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
            response.setStatus(429);
            response.setContentType("application/json");
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .code(ErrorCode.ERR_IAM_003_RATE_LIMITED.getHttpStatus())
                    .message(ErrorCode.ERR_IAM_003_RATE_LIMITED.getMessage())
                    .build();
            response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
