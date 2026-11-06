package org.udaan.solution;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SolutionController {

    private final SolutionService solutions;

    SolutionController(SolutionService solutions) {
        this.solutions = solutions;
    }

    record ProposeRequest(
            @NotBlank @Size(min = 10, max = 150, message = "must be 10 to 150 characters") String title,
            @NotBlank @Size(min = 30, max = 2000, message = "must be 30 to 2000 characters") String summary,
            @NotBlank @Size(min = 30, max = 5000, message = "must be 30 to 5000 characters") String approach,
            @DecimalMin("0") @DecimalMax("100000000000") BigDecimal estimatedCostInr,
            @Size(max = 200) String timelineText,
            @Size(max = 1000) String resourcesRequired,
            @Size(max = 15) List<@Size(max = 40) String> skills) {
    }

    @GetMapping("/api/v1/problems/{ref}/solutions")
    List<SolutionView> forProblem(@PathVariable String ref, @AuthenticationPrincipal Jwt jwt) {
        return solutions.forProblem(ref, viewer(jwt));
    }

    @PostMapping("/api/v1/problems/{ref}/solutions")
    @ResponseStatus(HttpStatus.CREATED)
    SolutionView propose(@PathVariable String ref, @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProposeRequest r) {
        return solutions.propose(ref, viewer(jwt), new Solution.Draft(r.title(), r.summary(), r.approach(),
                r.estimatedCostInr(), r.timelineText(), r.resourcesRequired(), r.skills()));
    }

    @GetMapping("/api/v1/solutions")
    List<SolutionView> recent(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
                              @AuthenticationPrincipal Jwt jwt) {
        return solutions.recent(page, size, viewer(jwt));
    }

    @GetMapping("/api/v1/solutions/{id}")
    SolutionView get(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return solutions.get(id, viewer(jwt));
    }

    @PostMapping("/api/v1/solutions/{id}/accept")
    SolutionView accept(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return solutions.accept(id, viewer(jwt));
    }

    @PostMapping("/api/v1/solutions/{id}/withdraw")
    SolutionView withdraw(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return solutions.withdraw(id, viewer(jwt));
    }

    @PutMapping("/api/v1/solutions/{id}/endorsement")
    SolutionService.Endorsement endorse(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return solutions.endorse(id, viewer(jwt), true);
    }

    @DeleteMapping("/api/v1/solutions/{id}/endorsement")
    SolutionService.Endorsement unendorse(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return solutions.endorse(id, viewer(jwt), false);
    }

    @GetMapping("/api/v1/me/solutions")
    List<SolutionView> mine(@AuthenticationPrincipal Jwt jwt) {
        return solutions.mine(viewer(jwt));
    }

    private static UUID viewer(Jwt jwt) {
        return jwt == null ? null : UUID.fromString(jwt.getSubject());
    }
}
