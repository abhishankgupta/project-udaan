package org.udaan.problem;

import java.time.Instant;
import java.util.UUID;

/** A problem in a list. */
record ProblemSummary(UUID id, String publicCode, String title, String categoryName, String localityName,
                      String cityName, Urgency urgency, ProblemStatus status, int supporters, int solutions,
                      String thumbnailUrl, Instant createdAt) {
}
