package org.udaan.problem.api;

import java.util.UUID;

/** What other modules need to know about a problem. {@code status} is a ProblemStatus name. */
public record ProblemRef(UUID id, String publicCode, String title, UUID submitterId, String status) {
}
