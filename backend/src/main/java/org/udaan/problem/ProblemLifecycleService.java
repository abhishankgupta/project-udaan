package org.udaan.problem;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.udaan.media.api.MediaFacade;
import org.udaan.shared.web.ApiException;
import org.udaan.shared.web.FieldException;
import org.udaan.shared.web.NotFoundException;

/** Status changes, progress updates and resolution votes on a public problem. */
@Service
class ProblemLifecycleService {

    private final ProblemRepository problems;
    private final ProblemTimeline timeline;
    private final ResolutionVotes votes;
    private final MediaFacade media;
    private final ProblemService problemService;

    ProblemLifecycleService(ProblemRepository problems, ProblemTimeline timeline, ResolutionVotes votes, MediaFacade media,
                            ProblemService problemService) {
        this.problems = problems;
        this.timeline = timeline;
        this.votes = votes;
        this.media = media;
        this.problemService = problemService;
    }

    @Transactional
    ProblemView transition(String ref, UUID actorId, ProblemStatus to, String note, List<UUID> mediaIds) {
        Problem problem = publicProblem(ref);
        Set<ProblemLifecycle.Role> roles = problem.rolesOf(actorId);
        if (roles.isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_ALLOWED",
                    "Only the person who raised this problem, or whoever is delivering its accepted solution, can change its status");
        }
        if (!ProblemLifecycle.allowed(problem.getStatus(), roles).contains(to)) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_TRANSITION",
                    "A problem that is " + label(problem.getStatus()) + " can't be moved to " + label(to));
        }
        String cleanNote = note == null || note.isBlank() ? null : note.trim();
        if (ProblemLifecycle.needsNote(to) && (cleanNote == null || cleanNote.length() < 10)) {
            throw new FieldException("note", to == ProblemStatus.RESOLVED
                    ? "Describe what was done to resolve it (at least 10 characters)"
                    : "Explain why it is being closed (at least 10 characters)");
        }
        List<UUID> photos = mediaIds == null ? List.of() : mediaIds;
        if (ProblemLifecycle.needsEvidence(to) && photos.isEmpty()) {
            throw new FieldException("evidenceMediaIds", "Add at least one photo showing the problem is resolved");
        }
        media.requireOwned(photos, actorId);

        if (problem.getStatus() == ProblemStatus.RESOLVED) {
            votes.clear(problem.getId());
        }
        timeline.move(problem, to, actorId, primaryRole(roles), cleanNote, photos);
        return problemService.get(problem.getPublicCode(), actorId);
    }

    @Transactional
    ProblemView postUpdate(String ref, UUID actorId, String body, List<UUID> mediaIds) {
        Problem problem = publicProblem(ref);
        Set<ProblemLifecycle.Role> roles = problem.rolesOf(actorId);
        if (roles.isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_ALLOWED",
                    "Only the person who raised this problem, or whoever is delivering its accepted solution, can post updates");
        }
        if (!ProblemLifecycle.acceptsUpdates(problem.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "PROBLEM_NOT_ACTIVE", "Updates can't be posted on a " + label(problem.getStatus()) + " problem");
        }
        List<UUID> photos = mediaIds == null ? List.of() : mediaIds;
        media.requireOwned(photos, actorId);
        timeline.progress(problem, actorId, primaryRole(roles), body.trim(), photos);
        return problemService.get(problem.getPublicCode(), actorId);
    }

    @Transactional
    ProblemView vote(String ref, UUID voterId, ResolutionVotes.Vote vote) {
        Problem problem = publicProblem(ref);
        if (!ResolutionVotes.votingOpen(problem)) {
            throw new ApiException(HttpStatus.CONFLICT, "VOTING_CLOSED", "This problem is not waiting for confirmation");
        }
        if (problem.rolesOf(voterId).contains(ProblemLifecycle.Role.OWNER)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OWNER_CANNOT_VOTE", "You marked this problem resolved, so others confirm it");
        }
        ResolutionVotes.Tally tally = votes.cast(problem.getId(), voterId, vote);
        if (tally.shouldReopen()) {
            votes.clear(problem.getId());
            timeline.move(problem, ProblemStatus.IN_PROGRESS, null, "COMMUNITY",
                    "Reopened: " + tally.disputes() + " people said the problem is not resolved", List.of());
        }
        return problemService.get(problem.getPublicCode(), voterId);
    }

    private Problem publicProblem(String ref) {
        Optional<Problem> found = ref.startsWith("UDN-") ? problems.findByPublicCode(ref) : parse(ref).flatMap(problems::findById);
        return found.filter(p -> p.getStatus().isPublic())
                .orElseThrow(() -> new NotFoundException("PROBLEM_NOT_FOUND", "This problem does not exist or is not public yet"));
    }

    private static String primaryRole(Set<ProblemLifecycle.Role> roles) {
        return roles.contains(ProblemLifecycle.Role.OWNER) ? "OWNER" : "ACCEPTED_PROPOSER";
    }

    private static String label(ProblemStatus s) {
        return s.name().toLowerCase().replace('_', ' ');
    }

    private static Optional<UUID> parse(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
