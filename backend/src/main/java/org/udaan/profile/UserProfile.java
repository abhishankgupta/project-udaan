package org.udaan.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Public profile details. Created lazily the first time a user edits their profile. */
@Entity
@Table(name = "user_profiles")
class UserProfile {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    private String bio;

    private String city;

    private String locality;

    /** Comma-separated; exposed as a list. */
    private String skills;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserProfile() {
        // for JPA
    }

    UserProfile(UUID userId) {
        this.userId = userId;
        this.updatedAt = Instant.now();
    }

    void update(String bio, String city, String locality, List<String> skills) {
        this.bio = blankToNull(bio);
        this.city = blankToNull(city);
        this.locality = blankToNull(locality);
        this.skills = skills == null ? null : blankToNull(String.join(",", skills.stream()
                .map(String::trim).filter(s -> !s.isEmpty()).distinct().limit(15).toList()));
        this.updatedAt = Instant.now();
    }

    String getBio() {
        return bio;
    }

    String getCity() {
        return city;
    }

    String getLocality() {
        return locality;
    }

    List<String> getSkills() {
        return skills == null ? List.of() : Arrays.stream(skills.split(",")).toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
