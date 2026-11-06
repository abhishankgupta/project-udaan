package org.udaan;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Minimal HTTP helper for integration tests against the running app. */
public final class TestApi {

    private final HttpClient http = HttpClient.newHttpClient();
    private final String base;

    public TestApi(int port) {
        this.base = "http://localhost:" + port;
    }

    public Response get(String path) {
        return send("GET", path, null, null);
    }

    public Response get(String path, String token) {
        return send("GET", path, null, token);
    }

    public Response post(String path, String json) {
        return send("POST", path, json, null);
    }

    public Response post(String path, String json, String token) {
        return send("POST", path, json, token);
    }

    public Response patch(String path, String json, String token) {
        return send("PATCH", path, json, token);
    }

    private Response send(String method, String path, String json, String token) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path))
                .method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json));
        if (json != null) {
            builder.header("Content-Type", "application/json");
        }
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        try {
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body());
        } catch (Exception e) {
            throw new IllegalStateException(method + " " + path + " failed", e);
        }
    }

    public record Response(int status, String body) {

        /** First string value of a JSON field anywhere in the body. */
        public String field(String name) {
            Matcher m = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(body);
            return m.find() ? m.group(1) : null;
        }

        @Override
        public String toString() {
            return status + " " + body;
        }
    }
}
