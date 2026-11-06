package org.udaan.identity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.udaan.shared.Ids;

@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    @Column(unique = true)
    private String email;

    @Column(name = "phone_e164", unique = true)
    private String phoneE164;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "phone_verified_at")
    private Instant phoneVerifiedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    protected User() {
        // for JPA
    }

    private User(String displayName) {
        Instant now = Instant.now();
        this.id = Ids.newId();
        this.displayName = displayName;
        this.status = UserStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
        this.roles.add(Role.INDIVIDUAL);
    }

    static User withEmail(String email, String passwordHash, String displayName) {
        User user = new User(displayName);
        user.email = email;
        user.passwordHash = passwordHash;
        return user;
    }

    static User withVerifiedPhone(String phoneE164, String displayName) {
        User user = new User(displayName);
        user.phoneE164 = phoneE164;
        user.phoneVerifiedAt = Instant.now();
        return user;
    }

    void markPhoneVerified() {
        this.phoneVerifiedAt = Instant.now();
        touch();
    }

    void rename(String newDisplayName) {
        this.displayName = newDisplayName;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneE164() {
        return phoneE164;
    }

    String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<Role> getRoles() {
        return roles.isEmpty() ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(roles);
    }
}
