package org.udaan;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Boots the full app against a real PostGIS database (provided by docker compose
 * locally and by a service container in CI), runs Flyway, and calls the API over HTTP.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"udaan.environment=ci", "udaan.version=test-build"})
class ApiSmokeTest {

    @Value("${local.server.port}")
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void healthIsUp() throws Exception {
        HttpResponse<String> response = get("/actuator/health");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"UP\"");
    }

    @Test
    void metaReportsEnvironmentAndVersion() throws Exception {
        HttpResponse<String> response = get("/api/v1/meta");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"environment\":\"ci\"").contains("\"version\":\"test-build\"");
    }

    @Test
    void categoriesAreSeededByFlyway() throws Exception {
        HttpResponse<String> response = get("/api/v1/categories");
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("DRAINAGE_WATER").contains("LOCAL_EMPLOYMENT");
    }

    @Test
    void writesRequireAuthentication() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/categories"))
                .POST(HttpRequest.BodyPublishers.ofString("{}"))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isIn(401, 403);
    }
}
