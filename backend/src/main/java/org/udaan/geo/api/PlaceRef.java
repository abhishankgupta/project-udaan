package org.udaan.geo.api;

/**
 * A resolved place for a problem. {@code localityId} is null when the user typed a locality
 * that isn't in the list.
 */
public record PlaceRef(int stateId, String stateName, int cityId, String cityCode, String cityName,
                       Integer localityId, String localityName) {
}
