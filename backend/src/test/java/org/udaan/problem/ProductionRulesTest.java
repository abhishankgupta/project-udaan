package org.udaan.problem;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.udaan.TestApi;
import org.udaan.TestApi.Response;

/** In production, new problems wait for moderation and phone sign-in stays off until SMS is connected. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"udaan.environment=production"})
class ProductionRulesTest {

    @Value("${local.server.port}")
    int port;

    @Test
    void newProblemsStayUnderReviewAndPrivateToTheirSubmitter() {
        TestApi api = new TestApi(port);
        String token = api.registerNewUser("Asha");
        Response created = api.post("/api/v1/problems",
                ProblemFlowTest.problemJson("\"localityText\":\"New Colony\"", "", false), token);
        assertThat(created.status()).as(created.toString()).isEqualTo(201);
        assertThat(created.field("status")).isEqualTo("UNDER_REVIEW");
        assertThat(created.body()).doesNotContain("Published automatically");

        String code = created.field("publicCode");
        assertThat(api.get("/api/v1/problems/" + code, token).status()).isEqualTo(200);
        assertThat(api.get("/api/v1/problems/" + code).status()).isEqualTo(404);
        assertThat(api.get("/api/v1/problems/" + code, api.registerNewUser("Other")).status()).isEqualTo(404);
        assertThat(api.get("/api/v1/problems?size=50").body()).as("not in the public feed").doesNotContain(code);
        assertThat(api.put("/api/v1/problems/" + code + "/support", api.registerNewUser("Fan")).status()).isEqualTo(404);
    }

    @Test
    void phoneSignInIsOffWithoutAnSmsProvider() {
        TestApi api = new TestApi(port);
        assertThat(api.get("/api/v1/auth/options").body()).contains("\"phoneOtpEnabled\":false");
        assertThat(api.post("/api/v1/auth/otp/request", "{\"phone\":\"9876543210\"}").status()).isEqualTo(503);
    }
}
