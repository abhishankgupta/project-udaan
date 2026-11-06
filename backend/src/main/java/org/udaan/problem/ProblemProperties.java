package org.udaan.problem;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param autoApproveOutsideProduction until the moderation dashboard ships (step 6), non-production
 *        environments publish new problems straight away so they can be tested. Production always
 *        holds them under review.
 * @param maxPerUserPerDay   submission rate limit (section 8.3)
 */
@ConfigurationProperties(prefix = "udaan.problems")
public record ProblemProperties(Boolean autoApproveOutsideProduction, Integer maxPerUserPerDay) {

    public ProblemProperties {
        autoApproveOutsideProduction = autoApproveOutsideProduction == null || autoApproveOutsideProduction;
        maxPerUserPerDay = maxPerUserPerDay == null ? 5 : maxPerUserPerDay;
    }
}
