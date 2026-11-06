package org.udaan.identity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OtpCodeRepository extends JpaRepository<OtpCode, UUID> {

    long countByPhoneE164AndCreatedAtAfter(String phoneE164, Instant since);

    Optional<OtpCode> findFirstByPhoneE164OrderByCreatedAtDesc(String phoneE164);
}
