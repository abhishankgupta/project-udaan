package org.udaan.problem;

import java.util.EnumSet;
import java.util.Set;

/** The PRD's problem lifecycle (section 5.5); transitions arrive with status tracking in step 5. */
public enum ProblemStatus {
    SUBMITTED,
    UNDER_REVIEW,
    VERIFIED,
    OPEN_FOR_SOLUTIONS,
    SOLUTION_PROPOSED,
    SOLUTION_ACCEPTED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    private static final Set<ProblemStatus> NOT_YET_PUBLIC = EnumSet.of(SUBMITTED, UNDER_REVIEW);

    /** Problems become publicly visible only after moderation approves them. */
    public boolean isPublic() {
        return !NOT_YET_PUBLIC.contains(this);
    }
}
