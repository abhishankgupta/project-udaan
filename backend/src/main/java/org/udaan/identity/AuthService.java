package org.udaan.identity;

import java.time.Instant;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.udaan.identity.api.AuthResponse;
import org.udaan.shared.UdaanProperties;
import org.udaan.shared.web.ApiException;

/** Registration, email/password sign-in and phone one-time-code sign-in. */
@Service
class AuthService {

    private final UserRepository users;
    private final OtpCodeRepository otpCodes;
    private final TokenService tokens;
    private final PasswordEncoder passwordEncoder;
    private final OtpSender otpSender;
    private final AuthProperties authProperties;
    private final UdaanProperties udaanProperties;

    AuthService(UserRepository users, OtpCodeRepository otpCodes, TokenService tokens, PasswordEncoder passwordEncoder,
                OtpSender otpSender, AuthProperties authProperties, UdaanProperties udaanProperties) {
        this.users = users;
        this.otpCodes = otpCodes;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.otpSender = otpSender;
        this.authProperties = authProperties;
        this.udaanProperties = udaanProperties;
    }

    record AuthOptions(boolean emailEnabled, boolean phoneOtpEnabled, boolean phoneOtpTestMode) {
    }

    record OtpIssued(String phone, long expiresInSeconds, String testCode) {
    }

    AuthOptions options() {
        return new AuthOptions(true, phoneOtpEnabled(), phoneOtpEnabled() && !otpSender.deliversToPhone());
    }

    @Transactional
    AuthResponse register(String rawEmail, String password, String displayName) {
        String email = normalizeEmail(rawEmail);
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_TAKEN", "An account with this email already exists");
        }
        User user = users.save(User.withEmail(email, passwordEncoder.encode(password), displayName.trim()));
        return tokens.issueForNewSession(user);
    }

    @Transactional
    AuthResponse login(String rawEmail, String password) {
        User user = users.findByEmail(normalizeEmail(rawEmail))
                .filter(u -> u.getPasswordHash() != null && passwordEncoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                        "Email or password is incorrect"));
        requireActive(user);
        return tokens.issueForNewSession(user);
    }

    @Transactional
    OtpIssued requestOtp(String rawPhone) {
        if (!phoneOtpEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "PHONE_LOGIN_UNAVAILABLE",
                    "Phone sign-in is not available yet. Please use email.");
        }
        String phone = normalizePhone(rawPhone);
        AuthProperties.Otp otp = authProperties.otp();
        Instant now = Instant.now();
        long recentSends = otpCodes.countByPhoneE164AndCreatedAtAfter(phone, now.minus(otp.sendWindow()));
        if (recentSends >= otp.maxSendsPerWindow()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "OTP_RATE_LIMITED",
                    "Too many codes requested. Please wait a few minutes and try again.");
        }
        String code = Hashing.sixDigitCode();
        otpCodes.save(new OtpCode(phone, codeHash(phone, code), now.plus(otp.ttl())));
        otpSender.send(phone, code);
        String testCode = otpSender.deliversToPhone() ? null : code;
        return new OtpIssued(PhoneNumbers.masked(phone), otp.ttl().toSeconds(), testCode);
    }

    /** Signs in with a code, creating the account on first use. Failed attempts are counted even though we throw. */
    @Transactional(noRollbackFor = ApiException.class)
    AuthResponse verifyOtp(String rawPhone, String code, String displayName) {
        String phone = normalizePhone(rawPhone);
        OtpCode otp = otpCodes.findFirstByPhoneE164OrderByCreatedAtDesc(phone)
                .filter(c -> c.isUsable(Instant.now()))
                .orElseThrow(AuthService::invalidCode);
        if (otp.getAttempts() >= authProperties.otp().maxAttempts()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "OTP_TOO_MANY_ATTEMPTS",
                    "Too many wrong codes. Please request a new one.");
        }
        if (!Hashing.constantTimeEquals(otp.getCodeHash(), codeHash(phone, code.trim()))) {
            otp.recordFailedAttempt();
            throw invalidCode();
        }
        otp.consume();

        User user = users.findByPhoneE164(phone).map(existing -> {
            existing.markPhoneVerified();
            return existing;
        }).orElseGet(() -> users.save(User.withVerifiedPhone(phone, defaultName(displayName, phone))));
        requireActive(user);
        return tokens.issueForNewSession(user);
    }

    private boolean phoneOtpEnabled() {
        boolean testModeAllowed = authProperties.otp().exposeCodeOutsideProduction()
                && !"production".equalsIgnoreCase(udaanProperties.environment());
        return otpSender.deliversToPhone() || testModeAllowed;
    }

    private static String codeHash(String phone, String code) {
        return Hashing.sha256Hex(phone + ":" + code);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizePhone(String rawPhone) {
        return PhoneNumbers.normalizeIndianMobile(rawPhone).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST,
                "INVALID_PHONE", "Enter a valid 10-digit Indian mobile number"));
    }

    private static String defaultName(String displayName, String phone) {
        if (displayName != null && !displayName.isBlank()) {
            return displayName.trim();
        }
        return "Udaan member " + phone.substring(phone.length() - 4);
    }

    private static void requireActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_SUSPENDED", "This account is suspended");
        }
    }

    private static ApiException invalidCode() {
        return new ApiException(HttpStatus.BAD_REQUEST, "OTP_INVALID", "That code is incorrect or has expired");
    }
}
