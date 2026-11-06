package org.udaan.problem;

import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.udaan.problem.api.ProblemStatusChanged;
import org.udaan.problem.api.ProblemUpdatePosted;

/** The one place that changes a problem's status: it records the timeline entry and announces the change. */
@Component
class ProblemTimeline {

    private final ProblemUpdateRepository updates;
    private final ApplicationEventPublisher events;

    ProblemTimeline(ProblemUpdateRepository updates, ApplicationEventPublisher events) {
        this.updates = updates;
        this.events = events;
    }

    void move(Problem problem, ProblemStatus to, UUID actorId, String actorRole, String note, List<UUID> mediaIds) {
        ProblemStatus from = problem.getStatus();
        problem.changeStatus(to);
        updates.save(ProblemUpdate.statusChange(problem.getId(), actorId, actorRole, from, to, note, mediaIds));
        events.publishEvent(new ProblemStatusChanged(problem.getId(), problem.getPublicCode(), problem.getTitle(),
                problem.getSubmitterId(), from.name(), to.name(), actorId, note));
    }

    void move(Problem problem, ProblemStatus to, UUID actorId, String note) {
        move(problem, to, actorId, null, note, List.of());
    }

    void progress(Problem problem, UUID actorId, String actorRole, String body, List<UUID> mediaIds) {
        updates.save(ProblemUpdate.progress(problem.getId(), actorId, actorRole, body, mediaIds));
        events.publishEvent(new ProblemUpdatePosted(problem.getId(), problem.getPublicCode(), problem.getTitle(),
                problem.getSubmitterId(), actorId, problem.isAnonymous() && problem.getSubmitterId().equals(actorId), body));
    }
}
