package com.astral.express.pccms.identity.repository;

import com.astral.express.pccms.identity.entity.OtpPurpose;
import com.astral.express.pccms.identity.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {
    Optional<OtpToken> findFirstByContactAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(String contact, OtpPurpose purpose);
}
