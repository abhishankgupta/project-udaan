package org.udaan.geo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "states")
class State {

    @Id
    private Integer id;

    @Column(nullable = false)
    private String code;

    @Column(name = "name_en", nullable = false)
    private String nameEn;

    protected State() {
    }

    Integer getId() {
        return id;
    }

    String getCode() {
        return code;
    }

    String getNameEn() {
        return nameEn;
    }
}
