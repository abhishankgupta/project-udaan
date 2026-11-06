package org.udaan.media;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.udaan.media.api.MediaFacade;
import org.udaan.media.api.MediaRef;
import org.udaan.shared.web.ApiException;

@Service
class MediaService implements MediaFacade {

    static final long MAX_UPLOAD_BYTES = 10L * 1024 * 1024;
    static final Set<String> ACCEPTED_TYPES = Set.of("image/jpeg", "image/png");

    private final MediaObjectRepository objects;
    private final MediaBlobRepository blobs;

    MediaService(MediaObjectRepository objects, MediaBlobRepository blobs) {
        this.objects = objects;
        this.blobs = blobs;
    }

    record StoredFile(String contentType, byte[] bytes) {
    }

    @Transactional
    MediaRef storeImage(UUID ownerId, String contentType, byte[] bytes) {
        if (contentType == null || !ACCEPTED_TYPES.contains(contentType.toLowerCase())) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_IMAGE",
                    "Please upload a JPEG or PNG photo");
        }
        if (bytes.length == 0 || bytes.length > MAX_UPLOAD_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_TOO_LARGE", "Photos must be under 10 MB");
        }
        ImageProcessor.Processed processed;
        try {
            processed = ImageProcessor.toJpeg(bytes);
        } catch (IOException | RuntimeException e) {
            processed = null;
        }
        if (processed == null) {
            throw new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_IMAGE",
                    "That file could not be read as a photo");
        }
        MediaObject media = objects.save(new MediaObject(ownerId, "image/jpeg", processed.bytes().length,
                processed.width(), processed.height()));
        blobs.save(new MediaBlob(media.getId(), processed.bytes()));
        return toRef(media);
    }

    @Transactional(readOnly = true)
    Optional<StoredFile> load(UUID id) {
        return objects.findById(id).flatMap(media -> blobs.findById(id)
                .map(blob -> new StoredFile(media.getContentType(), blob.getBytes())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaRef> requireOwned(List<UUID> mediaIds, UUID ownerId) {
        if (new HashSet<>(mediaIds).size() != mediaIds.size()) {
            throw invalidMedia();
        }
        Map<UUID, MediaObject> found = objects.findAllById(mediaIds).stream()
                .collect(Collectors.toMap(MediaObject::getId, Function.identity()));
        for (UUID id : mediaIds) {
            MediaObject media = found.get(id);
            if (media == null || !media.getOwnerId().equals(ownerId)) {
                throw invalidMedia();
            }
        }
        return mediaIds.stream().map(found::get).map(MediaService::toRef).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaRef> describe(List<UUID> mediaIds) {
        Map<UUID, MediaObject> found = objects.findAllById(mediaIds).stream()
                .collect(Collectors.toMap(MediaObject::getId, Function.identity()));
        return mediaIds.stream().filter(found::containsKey).map(found::get).map(MediaService::toRef).toList();
    }

    private static MediaRef toRef(MediaObject media) {
        return new MediaRef(media.getId(), MediaRef.urlFor(media.getId()), media.getWidth(), media.getHeight());
    }

    private static ApiException invalidMedia() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_MEDIA", "One of the photos could not be attached");
    }
}
