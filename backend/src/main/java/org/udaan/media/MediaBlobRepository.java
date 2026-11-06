package org.udaan.media;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface MediaBlobRepository extends JpaRepository<MediaBlob, UUID> {
}
