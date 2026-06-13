package com.astral.express.pccms.identity.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void should_BlacklistToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        tokenBlacklistService.blacklist("my-token", 3600);
        
        verify(valueOperations).set("jwt:blacklist:my-token", "1", 3600L, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    @Test
    void should_ReturnTrueIfBlacklisted() {
        when(redisTemplate.hasKey("jwt:blacklist:my-token")).thenReturn(true);
        boolean isBl = tokenBlacklistService.isBlacklisted("my-token");
        assertThat(isBl).isTrue();
    }

    @Test
    void should_ReturnFalseIfJtiIsNull_when_Blacklisting() {
        tokenBlacklistService.blacklist(null, 3600);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void should_ReturnFalseIfTtlIsNegative_when_Blacklisting() {
        tokenBlacklistService.blacklist("my-token", -1);
        verifyNoInteractions(redisTemplate);
    }

    @Test
    void should_ReturnFalseIfJtiIsNull_when_Checking() {
        boolean isBl = tokenBlacklistService.isBlacklisted(null);
        assertThat(isBl).isFalse();
    }
}
