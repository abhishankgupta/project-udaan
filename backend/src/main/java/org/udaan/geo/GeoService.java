package org.udaan.geo;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.udaan.geo.api.GeoFacade;
import org.udaan.geo.api.PlaceRef;

@Service
class GeoService implements GeoFacade {

    private final CityRepository cities;
    private final LocalityRepository localities;

    GeoService(CityRepository cities, LocalityRepository localities) {
        this.cities = cities;
        this.localities = localities;
    }

    @Transactional(readOnly = true)
    List<City> activeCities() {
        return cities.findByActiveTrueOrderByNameEnAsc();
    }

    @Transactional(readOnly = true)
    List<Locality> localitiesOf(int cityId) {
        return localities.findByCityIdOrderByNameEnAsc(cityId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlaceRef> resolve(int cityId, Integer localityId, String localityText) {
        Optional<City> city = cities.findById(cityId).filter(City::isActive);
        if (city.isEmpty()) {
            return Optional.empty();
        }
        City c = city.get();
        if (localityId != null) {
            return localities.findById(localityId)
                    .filter(l -> l.getCityId().equals(c.getId()))
                    .map(l -> place(c, l.getId(), l.getNameEn()));
        }
        String text = localityText == null || localityText.isBlank() ? null : localityText.trim();
        return Optional.of(place(c, null, text));
    }

    private static PlaceRef place(City c, Integer localityId, String localityName) {
        return new PlaceRef(c.getState().getId(), c.getState().getNameEn(), c.getId(), c.getCode(), c.getNameEn(),
                localityId, localityName);
    }
}
