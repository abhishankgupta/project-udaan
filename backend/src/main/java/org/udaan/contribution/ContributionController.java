package org.udaan.contribution;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ContributionController {

    private final ContributionService contributions;

    ContributionController(ContributionService contributions) {
        this.contributions = contributions;
    }

    record OfferRequest(
            @NotEmpty(message = "choose at least one way to help") Set<ContributionType> types,
            @Size(max = 1000) String message,
            UUID solutionId,
            Boolean shareContact) {
    }

    @GetMapping("/api/v1/problems/{ref}/contributions")
    List<ContributionView> forProblem(@PathVariable String ref, @AuthenticationPrincipal Jwt jwt) {
        return contributions.forProblem(ref, viewer(jwt));
    }

    @PostMapping("/api/v1/problems/{ref}/contributions")
    @ResponseStatus(HttpStatus.CREATED)
    ContributionView offer(@PathVariable String ref, @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OfferRequest r) {
        return contributions.offer(ref, viewer(jwt), r.solutionId(), r.types(), r.message(), Boolean.TRUE.equals(r.shareContact()));
    }

    @DeleteMapping("/api/v1/contributions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void withdraw(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        contributions.withdraw(id, viewer(jwt));
    }

    @GetMapping("/api/v1/me/contributions")
    List<ContributionView> mine(@AuthenticationPrincipal Jwt jwt) {
        return contributions.mine(viewer(jwt));
    }

    private static UUID viewer(Jwt jwt) {
        return jwt == null ? null : UUID.fromString(jwt.getSubject());
    }
}
