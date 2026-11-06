package org.udaan.media;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.udaan.shared.Ids;

/** Metadata for an uploaded photo. The bytes live separately so lists never load them. */
@Entity
@Table(name = "media_objects")
class MediaObject {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MediaObject() {
    }

    MediaObject(UUID ownerId, String contentType, int sizeBytes, int width, int height) {
        this.id = Ids.newId();
        this.ownerId = ownerId;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.width = width;
        this.height = height;
        this.createdAt = Instant.now();
    }

    UUID getId() {
        return id;
    }

    UUID getOwnerId() {
        return ownerId;
    }

    String getContentType() {
        return contentType;
    }

    int getWidth() {
        return width;
    }

    int getHeight() {
        return height;
    }
}
