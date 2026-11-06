package org.udaan.geo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "cities")
class City {

    @Id
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "state_id")
    private State state;

    @Column(nullable = false)
    private String code;

    @Column(name = "name_en", nullable = false)
    private String nameEn;

    @Column(nullable = false)
    private boolean active;

    protected City() {
    }

    Integer getId() {
        return id;
    }

    State getState() {
        return state;
    }

    String getCode() {
        return code;
    }

    String getNameEn() {
        return nameEn;
    }

    boolean isActive() {
        return active;
    }
}
