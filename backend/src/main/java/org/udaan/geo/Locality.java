package org.udaan.geo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "localities")
class Locality {

    @Id
    private Integer id;

    @Column(name = "city_id", nullable = false)
    private Integer cityId;

    @Column(name = "name_en", nullable = false)
    private String nameEn;

    protected Locality() {
    }

    Integer getId() {
        return id;
    }

    Integer getCityId() {
        return cityId;
    }

    String getNameEn() {
        return nameEn;
    }
}
