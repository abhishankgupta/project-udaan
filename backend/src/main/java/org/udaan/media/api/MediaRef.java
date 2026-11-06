package org.udaan.media.api;

import java.util.UUID;

/** A stored photo and the API path that serves it. */
public record MediaRef(UUID id, String url, int width, int height) {

    public static String urlFor(UUID id) {
        return "/api/v1/media/" + id;
    }
}
