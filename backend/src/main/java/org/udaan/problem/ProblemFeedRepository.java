package org.udaan.problem;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.udaan.media.api.MediaRef;

/**
 * Read-side queries for the feed, search and duplicate hints, written in SQL so filters, ranking
 * and search run in one database round trip. Only public problems are ever returned.
 */
@Repository
class ProblemFeedRepository {

    private static final String PUBLIC_ONLY = "p.status NOT IN ('SUBMITTED', 'UNDER_REVIEW')";

    private static final String SELECT = """
            SELECT p.id, p.public_code, p.title, c.name_en AS category_name,
                   COALESCE(l.name_en, p.locality_text) AS locality_name, ci.name_en AS city_name,
                   p.urgency, p.status, p.support_count, p.solution_count, p.created_at,
                   (SELECT pm.media_id FROM problem_media pm WHERE pm.problem_id = p.id ORDER BY pm.sort_order LIMIT 1) AS thumb,
                   CASE WHEN CAST(:viewer AS uuid) IS NULL THEN FALSE
                        ELSE EXISTS (SELECT 1 FROM problem_supports s WHERE s.problem_id = p.id AND s.user_id = CAST(:viewer AS uuid))
                   END AS supported_by_me
            FROM problems p
            JOIN categories c ON c.id = p.category_id
            JOIN cities ci ON ci.id = p.city_id
            LEFT JOIN localities l ON l.id = p.locality_id
            """;

    private static final String TRENDING = "(p.support_count + 2 * p.solution_count + 0.5 * p.comment_count + 1)"
            + " / power(EXTRACT(EPOCH FROM (now() - p.created_at)) / 3600 + 2, 1.5)";

    private static final String IMPACT = "(ln(1 + COALESCE(p.people_affected, 0))"
            + " + CASE p.urgency WHEN 'CRITICAL' THEN 4 WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 ELSE 1 END"
            + " + ln(1 + p.support_count))";

    private static final String RELEVANCE = "(ts_rank(p.search_vector, websearch_to_tsquery('simple', :q))"
            + " + word_similarity(:q, p.title))";

    private final JdbcClient jdbc;

    ProblemFeedRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    record Page(List<FeedItem> items, int page, int size, boolean hasMore) {
    }

    record FeedItem(UUID id, String publicCode, String title, String categoryName, String localityName,
                    String cityName, Urgency urgency, ProblemStatus status, int supporters, int solutions,
                    String thumbnailUrl, java.time.Instant createdAt, boolean supportedByMe) {
    }

    Page feed(ProblemFeedQuery query, UUID viewerId) {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE ").append(PUBLIC_ONLY);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("viewer", viewerId);

        if (query.cityId() != null) {
            sql.append(" AND p.city_id = :cityId");
            params.put("cityId", query.cityId());
        }
        if (query.localityId() != null) {
            sql.append(" AND p.locality_id = :localityId");
            params.put("localityId", query.localityId());
        }
        if (query.categoryId() != null) {
            sql.append(" AND p.category_id = :categoryId");
            params.put("categoryId", query.categoryId());
        }
        if (query.status() != null) {
            sql.append(" AND p.status = :status");
            params.put("status", query.status().name());
        }
        if (query.urgency() != null) {
            sql.append(" AND p.urgency = :urgency");
            params.put("urgency", query.urgency().name());
        }
        if (query.assistance() != null) {
            sql.append(" AND (',' || p.assistance_types || ',') LIKE :assistance");
            params.put("assistance", "%," + query.assistance().name() + ",%");
        }
        if (query.q() != null) {
            // Full-text match, typo-tolerant title match, or a plain substring (helps with Hindi and partial words).
            sql.append(" AND (p.search_vector @@ websearch_to_tsquery('simple', :q)"
                    + " OR word_similarity(:q, p.title) >= 0.5"
                    + " OR p.title ILIKE :qLike ESCAPE '\\' OR p.description ILIKE :qLike ESCAPE '\\')");
            params.put("q", query.q());
            params.put("qLike", "%" + escapeLike(query.q()) + "%");
        }

        sql.append(" ORDER BY ").append(switch (query.sort()) {
            case RECENT -> "p.created_at DESC";
            case SUPPORTED -> "p.support_count DESC, p.created_at DESC";
            case TRENDING -> TRENDING + " DESC, p.created_at DESC";
            case IMPACT -> IMPACT + " DESC, p.created_at DESC";
            case RELEVANCE -> RELEVANCE + " DESC, p.created_at DESC";
        });
        sql.append(" LIMIT :limit OFFSET :offset");
        params.put("limit", query.size() + 1);
        params.put("offset", query.page() * query.size());

        List<FeedItem> rows = new ArrayList<>(jdbc.sql(sql.toString()).params(params).query(ProblemFeedRepository::item).list());
        boolean hasMore = rows.size() > query.size();
        if (hasMore) {
            rows.remove(rows.size() - 1);
        }
        return new Page(rows, query.page(), query.size(), hasMore);
    }

    /** Public problems in the same city whose titles look like the one being written. */
    List<FeedItem> similar(String title, int cityId, Integer localityId) {
        String sql = SELECT + " WHERE " + PUBLIC_ONLY
                + " AND p.city_id = :cityId AND (similarity(p.title, :title) >= 0.2 OR word_similarity(:title, p.title) >= 0.5)"
                + " ORDER BY similarity(p.title, :title) + CASE WHEN p.locality_id = CAST(:localityId AS int) THEN 0.2 ELSE 0 END DESC"
                + " LIMIT 5";
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("viewer", null);
        params.put("cityId", cityId);
        params.put("localityId", localityId);
        params.put("title", title.trim());
        return jdbc.sql(sql).params(params).query(ProblemFeedRepository::item).list();
    }

    boolean supports(UUID problemId, UUID userId) {
        return exists("problem_supports", problemId, userId);
    }

    boolean follows(UUID problemId, UUID userId) {
        return exists("problem_follows", problemId, userId);
    }

    private boolean exists(String table, UUID problemId, UUID userId) {
        if (userId == null) {
            return false;
        }
        return jdbc.sql("SELECT EXISTS (SELECT 1 FROM " + table + " WHERE problem_id = :p AND user_id = :u)")
                .param("p", problemId).param("u", userId).query(Boolean.class).single();
    }

    private static FeedItem item(ResultSet rs, int rowNum) throws SQLException {
        UUID thumb = rs.getObject("thumb", UUID.class);
        return new FeedItem(
                rs.getObject("id", UUID.class),
                rs.getString("public_code"),
                rs.getString("title"),
                rs.getString("category_name"),
                rs.getString("locality_name"),
                rs.getString("city_name"),
                Urgency.valueOf(rs.getString("urgency")),
                ProblemStatus.valueOf(rs.getString("status")),
                rs.getInt("support_count"),
                rs.getInt("solution_count"),
                thumb == null ? null : MediaRef.urlFor(thumb),
                rs.getTimestamp("created_at").toInstant(),
                rs.getBoolean("supported_by_me"));
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
