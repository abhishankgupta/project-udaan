package org.udaan.identity;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Login settings.
 *
 * @param jwtSecret        HMAC key for access tokens, at least 32 characters. If blank, a random key
 *                         is generated at startup: access tokens then stop working after a restart, and
 *                         the web app silently gets new ones with its refresh token.
 * @param accessTokenTtl   lifetime of an access token
 * @param refreshTokenTtl  lifetime of a refresh token
 * @param otp              phone one-time-code settings
 */
@ConfigurationProperties(prefix = "udaan.auth")
public record AuthProperties(String jwtSecret, Duration accessTokenTtl, Duration refreshTokenTtl, Otp otp) {

    public AuthProperties {
        jwtSecret = jwtSecret == null ? "" : jwtSecret.trim();
        accessTokenTtl = accessTokenTtl == null ? Duration.ofMinutes(15) : accessTokenTtl;
        refreshTokenTtl = refreshTokenTtl == null ? Duration.ofDays(30) : refreshTokenTtl;
        otp = otp == null ? new Otp(null, null, null, null, null) : otp;
    }

    /**
     * @param ttl                how long a code stays valid
     * @param maxAttempts        wrong guesses allowed per code
     * @param maxSendsPerWindow  codes that may be sent to one number per window
     * @param sendWindow         rate-limit window
     * @param exposeCodeOutsideProduction when true, local/ci/test environments return the code in the
     *                           API response so the flow works before an SMS provider is connected.
     *                           Never applies in production.
     */
    public record Otp(Duration ttl, Integer maxAttempts, Integer maxSendsPerWindow, Duration sendWindow,
                      Boolean exposeCodeOutsideProduction) {

        public Otp {
            ttl = ttl == null ? Duration.ofMinutes(5) : ttl;
            maxAttempts = maxAttempts == null ? 5 : maxAttempts;
            maxSendsPerWindow = maxSendsPerWindow == null ? 3 : maxSendsPerWindow;
            sendWindow = sendWindow == null ? Duration.ofMinutes(15) : sendWindow;
            exposeCodeOutsideProduction = exposeCodeOutsideProduction == null || exposeCodeOutsideProduction;
        }
    }
}
