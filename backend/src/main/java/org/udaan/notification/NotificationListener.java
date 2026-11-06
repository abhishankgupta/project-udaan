package org.udaan.notification;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.udaan.contribution.api.HelpOffered;
import org.udaan.discussion.api.CommentPosted;
import org.udaan.identity.api.IdentityFacade;
import org.udaan.problem.api.ProblemFacade;
import org.udaan.problem.api.ProblemStatusChanged;
import org.udaan.problem.api.ProblemUpdatePosted;
import org.udaan.solution.api.SolutionAccepted;
import org.udaan.solution.api.SolutionProposed;

/**
 * Turns domain events into in-app notifications after the change has been committed, in a separate
 * transaction, so a notification failure never undoes the action itself (section 4.4).
 */
@Component
class NotificationListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationService notifications;
    private final ProblemFacade problems;
    private final IdentityFacade identity;

    NotificationListener(NotificationService notifications, ProblemFacade problems, IdentityFacade identity) {
        this.notifications = notifications;
        this.problems = problems;
        this.identity = identity;
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(SolutionProposed e) {
        safely(() -> notifications.send(withOwner(problems.audience(e.problemId()), e.problemOwnerId()), new NotificationService.Message(
                "SOLUTION_PROPOSED",
                name(e.proposerId()) + " proposed a solution to “" + e.problemTitle() + "”",
                e.solutionTitle(), "/solutions/" + e.solutionId(), e.proposerId(), e.problemId())));
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(SolutionAccepted e) {
        safely(() -> {
            notifications.send(List.of(e.proposerId()), new NotificationService.Message("SOLUTION_ACCEPTED",
                    "Your solution was accepted for “" + e.problemTitle() + "”",
                    "You can now post progress updates and mark work as started.", "/problems/" + e.problemCode(),
                    e.acceptedBy(), e.problemId()));
            Set<UUID> others = new HashSet<>(problems.audience(e.problemId()));
            others.remove(e.proposerId());
            notifications.send(others, new NotificationService.Message("SOLUTION_ACCEPTED",
                    "A solution was accepted for “" + e.problemTitle() + "”", e.solutionTitle(),
                    "/problems/" + e.problemCode(), e.acceptedBy(), e.problemId()));
        });
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ProblemStatusChanged e) {
        // Proposed/accepted have their own, more specific notifications.
        if ("SOLUTION_PROPOSED".equals(e.toStatus()) || "SOLUTION_ACCEPTED".equals(e.toStatus()) || "UNDER_REVIEW".equals(e.toStatus())) {
            return;
        }
        safely(() -> {
            if ("UNDER_REVIEW".equals(e.fromStatus())) {
                notifications.send(List.of(e.ownerId()), new NotificationService.Message("PROBLEM_PUBLISHED",
                        "Your problem is now public: “" + e.title() + "”", "Others can now support it and propose solutions.",
                        "/problems/" + e.publicCode(), null, e.problemId()));
                return;
            }
            notifications.send(problems.audience(e.problemId()), new NotificationService.Message("STATUS_CHANGED",
                    "“" + e.title() + "” is now " + label(e.toStatus()), e.note(), "/problems/" + e.publicCode(),
                    e.actorId(), e.problemId()));
        });
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(ProblemUpdatePosted e) {
        safely(() -> notifications.send(problems.audience(e.problemId()), new NotificationService.Message("PROGRESS_UPDATE",
                name(e.actorId()) + " posted an update on “" + e.title() + "”", e.body(), "/problems/" + e.publicCode(),
                e.actorId(), e.problemId())));
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(CommentPosted e) {
        safely(() -> {
            String author = name(e.authorId());
            if (e.parentAuthorId() != null) {
                notifications.send(List.of(e.parentAuthorId()), new NotificationService.Message("COMMENT_REPLY",
                        author + " replied to your comment on “" + e.targetTitle() + "”", e.excerpt(), e.link(),
                        e.authorId(), e.problemId()));
            }
            if (!e.targetOwnerId().equals(e.parentAuthorId())) {
                notifications.send(List.of(e.targetOwnerId()), new NotificationService.Message("COMMENT",
                        author + " commented on “" + e.targetTitle() + "”", e.excerpt(), e.link(), e.authorId(), e.problemId()));
            }
        });
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(HelpOffered e) {
        safely(() -> notifications.send(withOwner(Set.of(), e.problemOwnerId(), e.solutionProposerId()),
                new NotificationService.Message("HELP_OFFERED",
                        name(e.contributorId()) + " offered to help with “" + e.problemTitle() + "”",
                        "Open the problem to see their message.", "/problems/" + e.problemCode(), e.contributorId(),
                        e.problemId())));
    }

    private Set<UUID> withOwner(Set<UUID> audience, UUID... extra) {
        Set<UUID> all = new HashSet<>(audience);
        for (UUID id : extra) {
            if (id != null) {
                all.add(id);
            }
        }
        return all;
    }

    private String name(UUID userId) {
        return userId == null ? "Someone" : identity.findUser(userId).map(u -> u.displayName()).orElse("Someone");
    }

    private static String label(String status) {
        return status.toLowerCase().replace('_', ' ');
    }

    private static void safely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            log.warn("Could not create notifications", ex);
        }
    }
}
