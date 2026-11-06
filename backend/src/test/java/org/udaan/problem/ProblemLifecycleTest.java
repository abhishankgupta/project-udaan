package org.udaan.problem;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.udaan.problem.ProblemLifecycle.Role;

class ProblemLifecycleTest {

    @Test
    void ownersCanStartResolveAndCloseActiveProblems() {
        assertThat(ProblemLifecycle.allowed(ProblemStatus.SOLUTION_ACCEPTED, Set.of(Role.OWNER)))
                .containsExactlyInAnyOrder(ProblemStatus.IN_PROGRESS, ProblemStatus.RESOLVED, ProblemStatus.CLOSED);
        assertThat(ProblemLifecycle.allowed(ProblemStatus.IN_PROGRESS, Set.of(Role.OWNER)))
                .containsExactlyInAnyOrder(ProblemStatus.RESOLVED, ProblemStatus.CLOSED);
        assertThat(ProblemLifecycle.allowed(ProblemStatus.RESOLVED, Set.of(Role.OWNER))).containsExactly(ProblemStatus.IN_PROGRESS);
    }

    @Test
    void acceptedProposersCanOnlyStartWork() {
        assertThat(ProblemLifecycle.allowed(ProblemStatus.SOLUTION_ACCEPTED, Set.of(Role.ACCEPTED_PROPOSER)))
                .containsExactly(ProblemStatus.IN_PROGRESS);
        assertThat(ProblemLifecycle.allowed(ProblemStatus.IN_PROGRESS, Set.of(Role.ACCEPTED_PROPOSER))).isEmpty();
    }

    @Test
    void nobodyMovesProblemsThatAreUnderReviewOrClosed() {
        Set<Role> both = EnumSet.allOf(Role.class);
        assertThat(ProblemLifecycle.allowed(ProblemStatus.UNDER_REVIEW, both)).isEmpty();
        assertThat(ProblemLifecycle.allowed(ProblemStatus.CLOSED, both)).isEmpty();
        assertThat(ProblemLifecycle.allowed(ProblemStatus.OPEN_FOR_SOLUTIONS, Set.of())).isEmpty();
    }

    @Test
    void resolvingNeedsEvidenceAndANote() {
        assertThat(ProblemLifecycle.needsEvidence(ProblemStatus.RESOLVED)).isTrue();
        assertThat(ProblemLifecycle.needsNote(ProblemStatus.CLOSED)).isTrue();
        assertThat(ProblemLifecycle.needsEvidence(ProblemStatus.IN_PROGRESS)).isFalse();
        assertThat(ProblemLifecycle.acceptsUpdates(ProblemStatus.IN_PROGRESS)).isTrue();
        assertThat(ProblemLifecycle.acceptsUpdates(ProblemStatus.RESOLVED)).isFalse();
    }
}
