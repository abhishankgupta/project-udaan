package org.udaan.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.udaan.shared.Ids;

/** A hashed one-time code sent to a phone number. */
@Entity
@Table(name = "otp_codes")
class OtpCode {

    @Id
    private UUID id;

    @Column(name = "phone_e164", nullable = false)
    private String phoneE164;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OtpCode() {
        // for JPA
    }

    OtpCode(String phoneE164, String codeHash, Instant expiresAt) {
        this.id = Ids.newId();
        this.phoneE164 = phoneE164;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    boolean isUsable(Instant now) {
        return consumedAt == null && expiresAt.isAfter(now);
    }

    int getAttempts() {
        return attempts;
    }

    String getCodeHash() {
        return codeHash;
    }

    void recordFailedAttempt() {
        attempts++;
    }

    void consume() {
        consumedAt = Instant.now();
    }
}
