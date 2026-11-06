package org.udaan.problem;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Which status changes each kind of participant may make (the TransitionTable in section 6).
 * Moderator transitions arrive with the moderation dashboard.
 */
final class ProblemLifecycle {

    enum Role {
        /** The person who raised the problem. */
        OWNER,
        /** The proposer of the problem's accepted solution. */
        ACCEPTED_PROPOSER
    }

    private static final Set<ProblemStatus> ACTIVE = EnumSet.of(ProblemStatus.VERIFIED, ProblemStatus.OPEN_FOR_SOLUTIONS,
            ProblemStatus.SOLUTION_PROPOSED, ProblemStatus.SOLUTION_ACCEPTED, ProblemStatus.IN_PROGRESS);

    private static final Map<ProblemStatus, Set<ProblemStatus>> OWNER = Map.of(
            ProblemStatus.VERIFIED, EnumSet.of(ProblemStatus.IN_PROGRESS, ProblemStatus.RESOLVED, ProblemStatus.CLOSED),
            ProblemStatus.OPEN_FOR_SOLUTIONS, EnumSet.of(ProblemStatus.IN_PROGRESS, ProblemStatus.RESOLVED, ProblemStatus.CLOSED),
            ProblemStatus.SOLUTION_PROPOSED, EnumSet.of(ProblemStatus.IN_PROGRESS, ProblemStatus.RESOLVED, ProblemStatus.CLOSED),
            ProblemStatus.SOLUTION_ACCEPTED, EnumSet.of(ProblemStatus.IN_PROGRESS, ProblemStatus.RESOLVED, ProblemStatus.CLOSED),
            ProblemStatus.IN_PROGRESS, EnumSet.of(ProblemStatus.RESOLVED, ProblemStatus.CLOSED),
            ProblemStatus.RESOLVED, EnumSet.of(ProblemStatus.IN_PROGRESS));

    private static final Map<ProblemStatus, Set<ProblemStatus>> ACCEPTED_PROPOSER = Map.of(
            ProblemStatus.SOLUTION_ACCEPTED, EnumSet.of(ProblemStatus.IN_PROGRESS));

    private ProblemLifecycle() {
    }

    static Set<ProblemStatus> allowed(ProblemStatus from, Set<Role> roles) {
        EnumSet<ProblemStatus> result = EnumSet.noneOf(ProblemStatus.class);
        if (roles.contains(Role.OWNER)) {
            result.addAll(OWNER.getOrDefault(from, Set.of()));
        }
        if (roles.contains(Role.ACCEPTED_PROPOSER)) {
            result.addAll(ACCEPTED_PROPOSER.getOrDefault(from, Set.of()));
        }
        return result;
    }

    /** Progress updates are possible while a problem is being worked on. */
    static boolean acceptsUpdates(ProblemStatus status) {
        return ACTIVE.contains(status);
    }

    /** Resolving needs photo evidence; resolving and closing need an explanation. */
    static boolean needsEvidence(ProblemStatus to) {
        return to == ProblemStatus.RESOLVED;
    }

    static boolean needsNote(ProblemStatus to) {
        return to == ProblemStatus.RESOLVED || to == ProblemStatus.CLOSED;
    }
}
