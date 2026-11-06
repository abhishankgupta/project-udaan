package org.udaan.discussion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
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
class DiscussionController {

    private final DiscussionService discussion;

    DiscussionController(DiscussionService discussion) {
        this.discussion = discussion;
    }

    record PostRequest(@NotBlank @Size(max = 2000, message = "must be under 2000 characters") String body, UUID parentId) {
    }

    @GetMapping("/api/v1/problems/{ref}/comments")
    List<CommentView> problemComments(@PathVariable String ref, @AuthenticationPrincipal Jwt jwt) {
        return discussion.thread(discussion.problemTarget(ref), viewer(jwt));
    }

    @PostMapping("/api/v1/problems/{ref}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    CommentView postOnProblem(@PathVariable String ref, @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PostRequest r) {
        return discussion.post(discussion.problemTarget(ref), viewer(jwt), r.parentId(), r.body());
    }

    @GetMapping("/api/v1/solutions/{id}/comments")
    List<CommentView> solutionComments(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return discussion.thread(discussion.solutionTarget(id), viewer(jwt));
    }

    @PostMapping("/api/v1/solutions/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    CommentView postOnSolution(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PostRequest r) {
        return discussion.post(discussion.solutionTarget(id), viewer(jwt), r.parentId(), r.body());
    }

    @DeleteMapping("/api/v1/comments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        discussion.remove(id, viewer(jwt));
    }

    private static UUID viewer(Jwt jwt) {
        return jwt == null ? null : UUID.fromString(jwt.getSubject());
    }
}
