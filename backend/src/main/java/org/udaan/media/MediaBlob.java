package org.udaan.media;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "media_blobs")
class MediaBlob {

    @Id
    @Column(name = "media_id")
    private UUID mediaId;

    @Column(nullable = false)
    private byte[] bytes;

    protected MediaBlob() {
    }

    MediaBlob(UUID mediaId, byte[] bytes) {
        this.mediaId = mediaId;
        this.bytes = bytes;
    }

    byte[] getBytes() {
        return bytes;
    }
}
