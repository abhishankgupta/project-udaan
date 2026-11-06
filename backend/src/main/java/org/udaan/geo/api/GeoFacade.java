package org.udaan.geo.api;

import java.util.Optional;

public interface GeoFacade {

    /**
     * Resolves a city plus either a listed locality or free text. Empty when the city is unknown or
     * inactive, or the locality doesn't belong to the city.
     */
    Optional<PlaceRef> resolve(int cityId, Integer localityId, String localityText);
}
