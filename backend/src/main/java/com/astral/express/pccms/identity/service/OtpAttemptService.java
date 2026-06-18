package com.astral.express.pccms.identity.service;

import com.astral.express.pccms.identity.entity.OtpToken;
import com.astral.express.pccms.identity.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OtpAttemptService {
    private final OtpTokenRepository otpTokenRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementFailedAttempt(UUID tokenId) {
        OtpToken token = otpTokenRepository.findById(tokenId).orElseThrow();
        token.setAttemptCount(token.getAttemptCount() + 1);
        otpTokenRepository.save(token);
    }
}
