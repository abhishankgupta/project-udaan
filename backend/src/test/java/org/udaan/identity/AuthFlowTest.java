package org.udaan.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.udaan.TestApi;
import org.udaan.TestApi.Response;

/** End-to-end sign-in flows over HTTP against a real PostGIS database. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"udaan.environment=ci"})
class AuthFlowTest {

    @Value("${local.server.port}")
    int port;

    TestApi api;

    @BeforeEach
    void setUp() {
        api = new TestApi(port);
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.org";
    }

    private static String uniquePhone() {
        return "9" + String.format("%09d", ThreadLocalRandom.current().nextInt(1_000_000_000));
    }

    private Response register(String email, String password) {
        return api.post("/api/v1/auth/register",
                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"displayName\":\"Asha Verma\"}");
    }

    @Test
    void registerSignInAndManageProfile() {
        String email = uniqueEmail();
        Response registered = register(email, "correct-horse-1");
        assertThat(registered.status()).as(registered.toString()).isEqualTo(201);
        String token = registered.field("accessToken");
        String userId = registered.field("id");
        assertThat(token).isNotBlank();

        Response me = api.get("/api/v1/me", token);
        assertThat(me.status()).as(me.toString()).isEqualTo(200);
        assertThat(me.field("email")).isEqualTo(email);
        assertThat(me.body()).contains("INDIVIDUAL");

        Response updated = api.patch("/api/v1/me",
                "{\"displayName\":\"Asha V\",\"bio\":\"Civil engineer\",\"city\":\"Lucknow\",\"skills\":[\"Drainage\",\"Roads\"]}",
                token);
        assertThat(updated.status()).as(updated.toString()).isEqualTo(200);
        assertThat(updated.field("displayName")).isEqualTo("Asha V");
        assertThat(updated.body()).contains("Drainage").contains("Roads");

        Response publicProfile = api.get("/api/v1/users/" + userId);
        assertThat(publicProfile.status()).isEqualTo(200);
        assertThat(publicProfile.field("city")).isEqualTo("Lucknow");
        assertThat(publicProfile.body()).doesNotContain(email).doesNotContain("\"email\"").doesNotContain("\"phone\"");

        Response wrongPassword = api.post("/api/v1/auth/login",
                "{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}");
        assertThat(wrongPassword.status()).isEqualTo(401);
        assertThat(wrongPassword.field("code")).isEqualTo("INVALID_CREDENTIALS");

        Response login = api.post("/api/v1/auth/login",
                "{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"correct-horse-1\"}");
        assertThat(login.status()).as(login.toString()).isEqualTo(200);

        Response duplicate = register(email, "another-pass-1");
        assertThat(duplicate.status()).isEqualTo(409);
        assertThat(duplicate.field("code")).isEqualTo("EMAIL_TAKEN");
    }

    @Test
    void rejectsInvalidRegistration() {
        Response shortPassword = register(uniqueEmail(), "short");
        assertThat(shortPassword.status()).isEqualTo(400);
        assertThat(shortPassword.field("code")).isEqualTo("VALIDATION_FAILED");
        assertThat(shortPassword.body()).contains("password");
    }

    @Test
    void signedInEndpointsNeedAToken() {
        assertThat(api.get("/api/v1/me").status()).isEqualTo(401);
        assertThat(api.get("/api/v1/me", "not-a-real-token").status()).isEqualTo(401);
    }

    @Test
    void refreshTokensRotateAndReuseRevokesTheSession() {
        Response registered = register(uniqueEmail(), "correct-horse-1");
        String firstRefresh = registered.field("refreshToken");

        Response rotated = api.post("/api/v1/auth/refresh", "{\"refreshToken\":\"" + firstRefresh + "\"}");
        assertThat(rotated.status()).as(rotated.toString()).isEqualTo(200);
        String secondRefresh = rotated.field("refreshToken");
        assertThat(secondRefresh).isNotEqualTo(firstRefresh);
        assertThat(api.get("/api/v1/me", rotated.field("accessToken")).status()).isEqualTo(200);

        Response reused = api.post("/api/v1/auth/refresh", "{\"refreshToken\":\"" + firstRefresh + "\"}");
        assertThat(reused.status()).isEqualTo(401);

        Response afterReuse = api.post("/api/v1/auth/refresh", "{\"refreshToken\":\"" + secondRefresh + "\"}");
        assertThat(afterReuse.status()).as("whole family revoked after reuse").isEqualTo(401);
    }

    @Test
    void phoneCodeSignInCreatesAccountAndLogoutEndsSession() {
        Response options = api.get("/api/v1/auth/options");
        assertThat(options.body()).contains("\"phoneOtpEnabled\":true").contains("\"phoneOtpTestMode\":true");

        String phone = uniquePhone();
        Response issued = api.post("/api/v1/auth/otp/request", "{\"phone\":\"" + phone + "\"}");
        assertThat(issued.status()).as(issued.toString()).isEqualTo(200);
        String code = issued.field("testCode");
        assertThat(code).matches("\\d{6}");

        String wrong = code.equals("000000") ? "111111" : "000000";
        Response bad = api.post("/api/v1/auth/otp/verify", "{\"phone\":\"" + phone + "\",\"code\":\"" + wrong + "\"}");
        assertThat(bad.status()).isEqualTo(400);
        assertThat(bad.field("code")).isEqualTo("OTP_INVALID");

        Response ok = api.post("/api/v1/auth/otp/verify",
                "{\"phone\":\"+91 " + phone + "\",\"code\":\"" + code + "\",\"displayName\":\"Ravi\"}");
        assertThat(ok.status()).as(ok.toString()).isEqualTo(200);
        assertThat(ok.field("phone")).isEqualTo("+91" + phone);
        assertThat(ok.field("displayName")).isEqualTo("Ravi");

        Response reusedCode = api.post("/api/v1/auth/otp/verify", "{\"phone\":\"" + phone + "\",\"code\":\"" + code + "\"}");
        assertThat(reusedCode.status()).as("a code works only once").isEqualTo(400);

        String refresh = ok.field("refreshToken");
        assertThat(api.post("/api/v1/auth/logout", "{\"refreshToken\":\"" + refresh + "\"}").status()).isEqualTo(204);
        assertThat(api.post("/api/v1/auth/refresh", "{\"refreshToken\":\"" + refresh + "\"}").status()).isEqualTo(401);
    }

    @Test
    void phoneCodesAreRateLimited() {
        String phone = uniquePhone();
        for (int i = 0; i < 3; i++) {
            assertThat(api.post("/api/v1/auth/otp/request", "{\"phone\":\"" + phone + "\"}").status()).isEqualTo(200);
        }
        Response limited = api.post("/api/v1/auth/otp/request", "{\"phone\":\"" + phone + "\"}");
        assertThat(limited.status()).isEqualTo(429);
        assertThat(limited.field("code")).isEqualTo("OTP_RATE_LIMITED");
    }

    @Test
    void rejectsInvalidPhoneNumbers() {
        Response bad = api.post("/api/v1/auth/otp/request", "{\"phone\":\"12345\"}");
        assertThat(bad.status()).isEqualTo(400);
        assertThat(bad.field("code")).isEqualTo("INVALID_PHONE");
    }
}
