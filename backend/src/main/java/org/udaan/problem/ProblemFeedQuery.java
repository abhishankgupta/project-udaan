package org.udaan.problem;

import java.util.UUID;

/** Filters and ordering for the public feed (section 9). All filters are optional. */
record ProblemFeedQuery(String q, Integer cityId, Integer localityId, UUID categoryId, ProblemStatus status,
                        Urgency urgency, AssistanceType assistance, Sort sort, int page, int size) {

    enum Sort {
        RECENT,
        SUPPORTED,
        TRENDING,
        IMPACT,
        RELEVANCE
    }

    ProblemFeedQuery {
        q = q == null || q.isBlank() ? null : q.trim();
        if (sort == null) {
            sort = q != null ? Sort.RELEVANCE : Sort.RECENT;
        }
        if (sort == Sort.RELEVANCE && q == null) {
            sort = Sort.RECENT;
        }
        page = Math.max(0, page);
        size = Math.min(Math.max(1, size), 50);
    }
}
