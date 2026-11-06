package org.udaan.media.api;

import java.util.List;
import java.util.UUID;

public interface MediaFacade {

    /**
     * Returns the photos in the given order, or throws a 400 problem if any id is unknown or was not
     * uploaded by {@code ownerId}.
     */
    List<MediaRef> requireOwned(List<UUID> mediaIds, UUID ownerId);

    List<MediaRef> describe(List<UUID> mediaIds);
}
