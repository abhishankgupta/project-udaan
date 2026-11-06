package org.udaan.geo;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public location lists for forms and filters. */
@RestController
@RequestMapping("/api/v1/geo")
class GeoController {

    private final GeoService geo;

    GeoController(GeoService geo) {
        this.geo = geo;
    }

    record CityDto(int id, String code, String name, int stateId, String stateName) {
    }

    record LocalityDto(int id, String name) {
    }

    @GetMapping("/cities")
    List<CityDto> cities() {
        return geo.activeCities().stream()
                .map(c -> new CityDto(c.getId(), c.getCode(), c.getNameEn(), c.getState().getId(), c.getState().getNameEn()))
                .toList();
    }

    @GetMapping("/cities/{cityId}/localities")
    List<LocalityDto> localities(@PathVariable int cityId) {
        return geo.localitiesOf(cityId).stream().map(l -> new LocalityDto(l.getId(), l.getNameEn())).toList();
    }
}
