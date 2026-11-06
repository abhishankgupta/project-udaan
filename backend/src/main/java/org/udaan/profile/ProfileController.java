package org.udaan.profile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.udaan.identity.api.IdentityFacade;
import org.udaan.identity.api.UserSummary;
import org.udaan.shared.web.NotFoundException;

/** GET/PATCH /api/v1/me for the signed-in user, and public profiles at /api/v1/users/{id}. */
@RestController
class ProfileController {

    private final IdentityFacade identity;
    private final UserProfileRepository profiles;

    ProfileController(IdentityFacade identity, UserProfileRepository profiles) {
        this.identity = identity;
        this.profiles = profiles;
    }

    /** What the user sees about themself, including their own contact details. */
    record MyProfile(UUID id, String displayName, String email, String phone, List<String> roles,
                     String bio, String city, String locality, List<String> skills, Instant memberSince) {
    }

    /** What anyone can see. Never includes email or phone (section 13). */
    record PublicProfile(UUID id, String displayName, String bio, String city, String locality,
                         List<String> skills, Instant memberSince) {
    }

    record UpdateProfileRequest(
            @NotBlank @Size(min = 2, max = 80) String displayName,
            @Size(max = 500) String bio,
            @Size(max = 80) String city,
            @Size(max = 120) String locality,
            @Size(max = 15) List<@Size(max = 30) String> skills) {
    }

    @GetMapping("/api/v1/me")
    @Transactional(readOnly = true)
    MyProfile me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UserSummary user = identity.findUser(userId).orElseThrow(ProfileController::userNotFound);
        return toMine(user, profiles.findById(userId).orElse(null));
    }

    @PatchMapping("/api/v1/me")
    @Transactional
    MyProfile update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        UserSummary user = identity.rename(userId, request.displayName());
        UserProfile profile = profiles.findById(userId).orElseGet(() -> new UserProfile(userId));
        profile.update(request.bio(), request.city(), request.locality(), request.skills());
        profiles.save(profile);
        return toMine(user, profile);
    }

    @GetMapping("/api/v1/users/{id}")
    @Transactional(readOnly = true)
    PublicProfile publicProfile(@PathVariable UUID id) {
        UserSummary user = identity.findUser(id).orElseThrow(ProfileController::userNotFound);
        UserProfile profile = profiles.findById(id).orElse(null);
        return new PublicProfile(user.id(), user.displayName(),
                profile == null ? null : profile.getBio(),
                profile == null ? null : profile.getCity(),
                profile == null ? null : profile.getLocality(),
                profile == null ? List.of() : profile.getSkills(),
                user.memberSince());
    }

    private static MyProfile toMine(UserSummary user, UserProfile profile) {
        return new MyProfile(user.id(), user.displayName(), user.email(), user.phone(), user.roles(),
                profile == null ? null : profile.getBio(),
                profile == null ? null : profile.getCity(),
                profile == null ? null : profile.getLocality(),
                profile == null ? List.of() : profile.getSkills(),
                user.memberSince());
    }

    private static NotFoundException userNotFound() {
        return new NotFoundException("USER_NOT_FOUND", "User not found");
    }
}
