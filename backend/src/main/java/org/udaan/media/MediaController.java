package org.udaan.media;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.udaan.media.api.MediaRef;
import org.udaan.shared.web.ApiException;
import org.udaan.shared.web.NotFoundException;

/**
 * POST /api/v1/media uploads one photo (signed-in users); GET /api/v1/media/{id} serves it.
 * Photos are served by unguessable id so they work in plain {@code <img>} tags.
 */
@RestController
@RequestMapping("/api/v1/media")
class MediaController {

    private final MediaService media;

    MediaController(MediaService media) {
        this.media = media;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    MediaRef upload(@AuthenticationPrincipal Jwt jwt, @RequestParam("file") MultipartFile file) {
        try {
            return media.storeImage(UUID.fromString(jwt.getSubject()), file.getContentType(), file.getBytes());
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UPLOAD_FAILED", "The upload did not complete. Please try again.");
        }
    }

    @GetMapping("/{id}")
    ResponseEntity<byte[]> get(@PathVariable UUID id) {
        MediaService.StoredFile file = media.load(id)
                .orElseThrow(() -> new NotFoundException("MEDIA_NOT_FOUND", "Photo not found"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(file.bytes());
    }
}
