package org.udaan.problem;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Supports and follows, with their counters kept in step by atomic SQL (section 5.3). An insert that
 * finds an existing row changes nothing, so repeated clicks are safe.
 */
@Repository
class ProblemCounters {

    private final JdbcClient jdbc;

    ProblemCounters(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Counters other modules may move. Column names are fixed here, never taken from input. */
    enum Counter {
        SOLUTIONS("solution_count"),
        COMMENTS("comment_count");

        final String column;

        Counter(String column) {
            this.column = column;
        }
    }

    void adjust(UUID problemId, Counter counter, int delta) {
        jdbc.sql("UPDATE problems SET " + counter.column + " = GREATEST(0, " + counter.column + " + :d) WHERE id = :p")
                .param("d", delta).param("p", problemId).update();
    }

    int addSupport(UUID problemId, UUID userId) {
        return toggle("problem_supports", "support_count", problemId, userId, true);
    }

    int removeSupport(UUID problemId, UUID userId) {
        return toggle("problem_supports", "support_count", problemId, userId, false);
    }

    int addFollow(UUID problemId, UUID userId) {
        return toggle("problem_follows", "follower_count", problemId, userId, true);
    }

    int removeFollow(UUID problemId, UUID userId) {
        return toggle("problem_follows", "follower_count", problemId, userId, false);
    }

    /** Returns the counter's value after the change. Table and column names are fixed constants above. */
    private int toggle(String table, String counter, UUID problemId, UUID userId, boolean add) {
        String change = add
                ? "INSERT INTO " + table + " (problem_id, user_id) VALUES (:p, :u) ON CONFLICT DO NOTHING"
                : "DELETE FROM " + table + " WHERE problem_id = :p AND user_id = :u";
        int changed = jdbc.sql(change).param("p", problemId).param("u", userId).update();
        if (changed == 1) {
            String delta = add ? "+ 1" : "- 1";
            return jdbc.sql("UPDATE problems SET " + counter + " = GREATEST(0, " + counter + " " + delta + ") WHERE id = :p RETURNING " + counter)
                    .param("p", problemId).query(Integer.class).single();
        }
        return jdbc.sql("SELECT " + counter + " FROM problems WHERE id = :p").param("p", problemId).query(Integer.class).single();
    }
}
