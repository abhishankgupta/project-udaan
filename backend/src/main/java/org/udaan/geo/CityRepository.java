package org.udaan.geo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface CityRepository extends JpaRepository<City, Integer> {

    List<City> findByActiveTrueOrderByNameEnAsc();
}
