package org.udaan.geo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface LocalityRepository extends JpaRepository<Locality, Integer> {

    List<Locality> findByCityIdOrderByNameEnAsc(Integer cityId);
}
