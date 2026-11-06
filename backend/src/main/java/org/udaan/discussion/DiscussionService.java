package org.udaan.discussion;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.udaan.identity.api.IdentityFacade;
import org.udaan.problem.api.ProblemFacade;
import org.udaan.problem.api.ProblemRef;
import org.udaan.shared.web.ApiException;
import org.udaan.shared.web.NotFoundException;
import org.udaan.solution.api.SolutionFacade;
import org.udaan.solution.api.SolutionRef;

@Service
class DiscussionService {

    static final int MAX_PER_HOUR = 60;

    private final CommentRepository comments;
    private final ProblemFacade problems;
    private final SolutionFacade solutions;
    private final IdentityFacade identity;

    DiscussionService(CommentRepository comments, ProblemFacade problems, SolutionFacade solutions, IdentityFacade identity) {
        this.comments = comments;
        this.problems = problems;
        this.solutions = solutions;
        this.identity = identity;
    }

    record Target(Comment.TargetType type, UUID targetId, UUID problemId) {
    }

    Target problemTarget(String ref) {
        ProblemRef p = problems.requirePublic(ref);
        return new Target(Comment.TargetType.PROBLEM, p.id(), p.id());
    }

    Target solutionTarget(UUID solutionId) {
        SolutionRef s = solutions.requireVisible(solutionId);
        return new Target(Comment.TargetType.SOLUTION, s.id(), s.problemId());
    }

    @Transactional(readOnly = true)
    List<CommentView> thread(Target target, UUID viewerId) {
        List<Comment> all = comments.findByTargetTypeAndTargetIdOrderByCreatedAtAsc(target.type(), target.targetId());
        Map<UUID, String> names = new HashMap<>();
        Map<UUID, List<CommentView>> replies = new HashMap<>();
        for (Comment c : all) {
            if (c.getParentId() != null) {
                replies.computeIfAbsent(c.getParentId(), k -> new ArrayList<>()).add(view(c, viewerId, names, List.of()));
            }
        }
        Map<UUID, CommentView> topLevel = new LinkedHashMap<>();
        for (Comment c : all) {
            if (c.getParentId() == null) {
                List<CommentView> children = replies.getOrDefault(c.getId(), List.of());
                // A removed comment with no replies has nothing left to show.
                if (c.getStatus() == Comment.Status.REMOVED && children.stream().allMatch(CommentView::removed)) {
                    continue;
                }
                topLevel.put(c.getId(), view(c, viewerId, names, children));
            }
        }
        return List.copyOf(topLevel.values());
    }

    @Transactional
    CommentView post(Target target, UUID authorId, UUID parentId, String body) {
        if (comments.countByAuthorIdAndCreatedAtAfter(authorId, Instant.now().minus(Duration.ofHours(1))) >= MAX_PER_HOUR) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "COMMENT_RATE_LIMITED",
                    "You're commenting very quickly. Please wait a little and try again.");
        }
        if (parentId != null) {
            Comment parent = comments.findById(parentId)
                    .filter(p -> p.getTargetType() == target.type() && p.getTargetId().equals(target.targetId()))
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PARENT", "You can only reply to a comment in this discussion"));
            if (parent.getParentId() != null) {
                parentId = parent.getParentId(); // keep threads one level deep
            }
            if (parent.getStatus() == Comment.Status.REMOVED && parent.getParentId() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PARENT", "That comment was removed");
            }
        }
        Comment comment = comments.save(Comment.post(target.type(), target.targetId(), target.problemId(), parentId, authorId, body));
        problems.commentCountChanged(target.problemId(), 1);
        return view(comment, authorId, new HashMap<>(), List.of());
    }

    @Transactional
    void remove(UUID commentId, UUID actorId) {
        Comment comment = comments.findById(commentId)
                .filter(c -> c.getStatus() == Comment.Status.VISIBLE)
                .orElseThrow(() -> new NotFoundException("COMMENT_NOT_FOUND", "Comment not found"));
        if (!comment.getAuthorId().equals(actorId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_AUTHOR", "You can only delete your own comments");
        }
        comment.remove();
        problems.commentCountChanged(comment.getProblemId(), -1);
    }

    private CommentView view(Comment c, UUID viewerId, Map<UUID, String> names, List<CommentView> replies) {
        if (c.getStatus() == Comment.Status.REMOVED) {
            return new CommentView(c.getId(), null, null, null, true, false, c.getCreatedAt(), replies);
        }
        String name = names.computeIfAbsent(c.getAuthorId(),
                id -> identity.findUser(id).map(u -> u.displayName()).orElse("Udaan member"));
        return new CommentView(c.getId(), c.getAuthorId(), name, c.getBody(), false, c.getAuthorId().equals(viewerId),
                c.getCreatedAt(), replies);
    }
}
