package org.udaan.problem;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ProblemController {

    private final ProblemService problems;

    ProblemController(ProblemService problems) {
        this.problems = problems;
    }

    /** Location fields. Coordinates are optional and limited to India's bounding box. */
    record SubmitProblemRequest(
            @NotBlank @Size(min = 10, max = 150, message = "must be 10 to 150 characters") String title,
            @NotBlank @Size(min = 30, max = 5000, message = "must be 30 to 5000 characters") String description,
            @NotNull UUID categoryId,
            @NotNull Integer cityId,
            Integer localityId,
            @Size(max = 120) String localityText,
            @Size(max = 200) String addressText,
            @DecimalMin("6.0") @DecimalMax("37.5") Double latitude,
            @DecimalMin("68.0") @DecimalMax("97.5") Double longitude,
            @NotNull Urgency urgency,
            @Min(1) @Max(100_000_000) Integer peopleAffected,
            @Size(max = 500) String estimatedImpact,
            @NotBlank @Size(min = 10, max = 500, message = "must be 10 to 500 characters") String desiredOutcome,
            @NotEmpty(message = "choose at least one kind of help") Set<AssistanceType> assistanceTypes,
            boolean anonymous,
            @Size(max = 4, message = "add up to 4 photos") List<UUID> mediaIds) {
    }

    @PostMapping("/api/v1/problems")
    @ResponseStatus(HttpStatus.CREATED)
    ProblemView submit(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SubmitProblemRequest r) {
        return problems.submit(UUID.fromString(jwt.getSubject()), new ProblemService.SubmitCommand(
                r.title(), r.description(), r.categoryId(), r.cityId(), r.localityId(), r.localityText(),
                r.addressText(), r.latitude(), r.longitude(), r.urgency(), r.peopleAffected(), r.estimatedImpact(),
                r.desiredOutcome(), r.assistanceTypes(), r.anonymous(), r.mediaIds()));
    }

    /** By public code (UDN-LKO-000123) or id. Unpublished problems are visible only to their submitter. */
    @GetMapping("/api/v1/problems/{ref}")
    ProblemView get(@PathVariable String ref, @AuthenticationPrincipal Jwt jwt) {
        return problems.get(ref, jwt == null ? null : UUID.fromString(jwt.getSubject()));
    }

    @GetMapping("/api/v1/me/problems")
    List<ProblemSummary> mine(@AuthenticationPrincipal Jwt jwt) {
        return problems.mine(UUID.fromString(jwt.getSubject()));
    }
}
