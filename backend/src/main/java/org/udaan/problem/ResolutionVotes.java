package org.udaan.problem;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Community confirmations and disputes of a resolution, open for 14 days after it is marked resolved. */
@Component
class ResolutionVotes {

    static final Duration VOTING_WINDOW = Duration.ofDays(14);
    static final int DISPUTES_TO_REOPEN = 3;

    private final JdbcClient jdbc;

    ResolutionVotes(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    enum Vote { CONFIRM, DISPUTE }

    record Tally(int confirms, int disputes) {
        boolean shouldReopen() {
            return disputes >= DISPUTES_TO_REOPEN && disputes > confirms;
        }
    }

    static boolean votingOpen(Problem p) {
        return p.getStatus() == ProblemStatus.RESOLVED && p.getResolvedAt() != null
                && Instant.now().isBefore(p.getResolvedAt().plus(VOTING_WINDOW));
    }

    Tally cast(UUID problemId, UUID userId, Vote vote) {
        jdbc.sql("""
                INSERT INTO problem_resolution_votes (problem_id, user_id, vote) VALUES (:p, :u, :v)
                ON CONFLICT (problem_id, user_id) DO UPDATE SET vote = EXCLUDED.vote, created_at = now()
                """).param("p", problemId).param("u", userId).param("v", vote.name()).update();
        return tally(problemId);
    }

    /** Votes belong to one resolution; reopening clears them so a later resolution starts fresh. */
    void clear(UUID problemId) {
        jdbc.sql("DELETE FROM problem_resolution_votes WHERE problem_id = :p").param("p", problemId).update();
    }

    Tally tally(UUID problemId) {
        return jdbc.sql("""
                SELECT count(*) FILTER (WHERE vote = 'CONFIRM') AS confirms, count(*) FILTER (WHERE vote = 'DISPUTE') AS disputes
                FROM problem_resolution_votes WHERE problem_id = :p
                """).param("p", problemId)
                .query((rs, n) -> new Tally(rs.getInt("confirms"), rs.getInt("disputes"))).single();
    }

    ProblemView.Resolution summary(Problem p, UUID viewerId) {
        Tally t = tally(p.getId());
        String mine = viewerId == null ? null : jdbc.sql("SELECT vote FROM problem_resolution_votes WHERE problem_id = :p AND user_id = :u")
                .param("p", p.getId()).param("u", viewerId).query(String.class).optional().orElse(null);
        Instant closes = p.getResolvedAt() == null ? null : p.getResolvedAt().plus(VOTING_WINDOW);
        return new ProblemView.Resolution(t.confirms(), t.disputes(), mine, votingOpen(p), closes);
    }
}
