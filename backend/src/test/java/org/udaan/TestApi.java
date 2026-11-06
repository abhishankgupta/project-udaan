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

    /** Uploads one file as multipart/form-data under the field name "file". */
    public Response upload(String path, String filename, String contentType, byte[] bytes, String token) {
        String boundary = "----udaan" + System.nanoTime();
        java.io.ByteArrayOutputStream body = new java.io.ByteArrayOutputStream();
        String head = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n";
        body.writeBytes(head.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        body.writeBytes(bytes);
        body.writeBytes(("\r\n--" + boundary + "--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return execute(builder, "POST", path);
    }

    /** GET returning the raw response, for binary bodies. */
    public HttpResponse<byte[]> getBytes(String path) {
        try {
            return http.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("GET " + path + " failed", e);
        }
    }

    /** Registers a fresh email account and returns its access token. */
    public String registerNewUser(String displayName) {
        String email = "user-" + java.util.UUID.randomUUID() + "@example.org";
        Response r = post("/api/v1/auth/register",
                "{\"email\":\"" + email + "\",\"password\":\"correct-horse-1\",\"displayName\":\"" + displayName + "\"}");
        if (r.status() != 201) {
            throw new IllegalStateException("register failed: " + r);
        }
        return r.field("accessToken");
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
        return execute(builder, method, path);
    }

    private Response execute(HttpRequest.Builder builder, String method, String path) {
        try {
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body());
        } catch (Exception e) {
            throw new IllegalStateException(method + " " + path + " failed", e);
        }
    }

    public record Response(int status, String body) {

        /** First number value of a JSON field anywhere in the body. */
        public Long number(String name) {
            Matcher m = Pattern.compile("\"" + Pattern.quote(name) + "\"\\s*:\\s*(-?\\d+)").matcher(body);
            return m.find() ? Long.parseLong(m.group(1)) : null;
        }

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
