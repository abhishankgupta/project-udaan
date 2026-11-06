package org.udaan.problem;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.udaan.geo.api.GeoFacade;
import org.udaan.geo.api.PlaceRef;
import org.udaan.identity.api.IdentityFacade;
import org.udaan.media.api.MediaFacade;
import org.udaan.media.api.MediaRef;
import org.udaan.shared.UdaanProperties;
import org.udaan.shared.web.ApiException;
import org.udaan.shared.web.FieldException;
import org.udaan.shared.web.NotFoundException;
import org.udaan.taxonomy.api.CategoryRef;
import org.udaan.taxonomy.api.TaxonomyFacade;

@Service
class ProblemService {

    private final ProblemRepository problems;
    private final ProblemUpdateRepository updates;
    private final TaxonomyFacade taxonomy;
    private final GeoFacade geo;
    private final MediaFacade media;
    private final IdentityFacade identity;
    private final ProblemProperties properties;
    private final UdaanProperties udaan;
    private final ProblemFeedRepository feed;
    private final ProblemCounters counters;

    ProblemService(ProblemRepository problems, ProblemUpdateRepository updates, TaxonomyFacade taxonomy,
                   GeoFacade geo, MediaFacade media, IdentityFacade identity, ProblemProperties properties,
                   UdaanProperties udaan, ProblemFeedRepository feed, ProblemCounters counters) {
        this.feed = feed;
        this.counters = counters;
        this.problems = problems;
        this.updates = updates;
        this.taxonomy = taxonomy;
        this.geo = geo;
        this.media = media;
        this.identity = identity;
        this.properties = properties;
        this.udaan = udaan;
    }

    record SubmitCommand(String title, String description, UUID categoryId, int cityId, Integer localityId,
                         String localityText, String addressText, Double latitude, Double longitude, Urgency urgency,
                         Integer peopleAffected, String estimatedImpact, String desiredOutcome,
                         java.util.Set<AssistanceType> assistanceTypes, boolean anonymous, List<UUID> mediaIds) {
    }

    @Transactional
    ProblemView submit(UUID submitterId, SubmitCommand cmd) {
        long recent = problems.countBySubmitterIdAndCreatedAtAfter(submitterId, Instant.now().minus(Duration.ofDays(1)));
        if (recent >= properties.maxPerUserPerDay()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "PROBLEM_RATE_LIMITED",
                    "You can raise up to " + properties.maxPerUserPerDay() + " problems a day. Please try again tomorrow.");
        }
        if (taxonomy.findActiveCategory(cmd.categoryId()).isEmpty()) {
            throw badField("categoryId", "Choose a category from the list");
        }
        if (cmd.localityId() == null && (cmd.localityText() == null || cmd.localityText().isBlank())) {
            throw badField("localityId", "Choose your locality or type its name");
        }
        PlaceRef place = geo.resolve(cmd.cityId(), cmd.localityId(), cmd.localityText())
                .orElseThrow(() -> badField("cityId", "Choose a city and locality from the list"));
        if ((cmd.latitude() == null) != (cmd.longitude() == null)) {
            throw badField("latitude", "Location needs both latitude and longitude");
        }
        List<UUID> mediaIds = cmd.mediaIds() == null ? List.of() : cmd.mediaIds();
        media.requireOwned(mediaIds, submitterId);

        String code = "UDN-" + place.cityCode() + "-" + String.format("%06d", problems.nextCodeNumber());
        Problem problem = problems.save(Problem.submit(code, submitterId, new Problem.Draft(
                cmd.title(), cmd.description(), cmd.categoryId(), place.stateId(), place.cityId(), place.localityId(),
                cmd.localityText(), cmd.addressText(), cmd.latitude(), cmd.longitude(), cmd.urgency(),
                cmd.peopleAffected(), cmd.estimatedImpact(), cmd.desiredOutcome(), cmd.assistanceTypes(),
                cmd.anonymous(), mediaIds)));

        moveTo(problem, ProblemStatus.UNDER_REVIEW, submitterId, "Submitted for review");
        if (autoApprove()) {
            moveTo(problem, ProblemStatus.OPEN_FOR_SOLUTIONS, null,
                    "Published automatically (" + udaan.environment() + " environment; moderation arrives later)");
        }
        return view(problem, submitterId);
    }

    @Transactional(readOnly = true)
    ProblemView get(String ref, UUID viewerId) {
        Problem problem = find(ref).filter(p -> p.isVisibleTo(viewerId)).orElseThrow(ProblemService::notFound);
        return view(problem, viewerId);
    }

    @Transactional(readOnly = true)
    List<ProblemSummary> mine(UUID userId) {
        return problems.findBySubmitterIdOrderByCreatedAtDesc(userId).stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    ProblemFeedRepository.Page feed(ProblemFeedQuery query, UUID viewerId) {
        return feed.feed(query, viewerId);
    }

    @Transactional(readOnly = true)
    List<ProblemFeedRepository.FeedItem> similar(String title, int cityId, Integer localityId) {
        if (title == null || title.trim().length() < 4) {
            return List.of();
        }
        return feed.similar(title, cityId, localityId);
    }

    record Toggle(boolean active, int count) {
    }

    @Transactional
    Toggle support(String ref, UUID userId, boolean add) {
        Problem problem = publicProblem(ref);
        if (add && problem.getSubmitterId().equals(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CANNOT_SUPPORT_OWN",
                    "You raised this problem, so your support is already counted in spirit. Share it to gather more.");
        }
        int count = add ? counters.addSupport(problem.getId(), userId) : counters.removeSupport(problem.getId(), userId);
        return new Toggle(add, count);
    }

    @Transactional
    Toggle follow(String ref, UUID userId, boolean add) {
        Problem problem = publicProblem(ref);
        int count = add ? counters.addFollow(problem.getId(), userId) : counters.removeFollow(problem.getId(), userId);
        return new Toggle(add, count);
    }

    private Problem publicProblem(String ref) {
        return find(ref).filter(p -> p.getStatus().isPublic()).orElseThrow(ProblemService::notFound);
    }

    private Optional<Problem> find(String ref) {
        return ref.startsWith("UDN-") ? problems.findByPublicCode(ref) : parseUuid(ref).flatMap(problems::findById);
    }

    private boolean autoApprove() {
        return properties.autoApproveOutsideProduction() && !"production".equalsIgnoreCase(udaan.environment());
    }

    private void moveTo(Problem problem, ProblemStatus to, UUID actorId, String note) {
        ProblemStatus from = problem.getStatus();
        problem.changeStatus(to);
        updates.save(ProblemUpdate.statusChange(problem.getId(), actorId, from, to, note));
    }

    private ProblemView view(Problem p, UUID viewerId) {
        boolean owner = p.getSubmitterId().equals(viewerId);
        CategoryRef category = taxonomy.findActiveCategory(p.getCategoryId())
                .orElse(new CategoryRef(p.getCategoryId(), "UNKNOWN", "Other", "अन्य"));
        PlaceRef place = geo.resolve(p.getCityId(), p.getLocalityId(), p.getLocalityText()).orElse(null);

        ProblemView.Submitter submitter;
        if (p.isAnonymous() && !owner) {
            submitter = new ProblemView.Submitter(null, null, true);
        } else {
            String name = identity.findUser(p.getSubmitterId()).map(u -> u.displayName()).orElse("Udaan member");
            submitter = new ProblemView.Submitter(p.getSubmitterId(), name, p.isAnonymous());
        }

        List<ProblemView.TimelineEntry> timeline = updates.findByProblemIdOrderByCreatedAtAsc(p.getId()).stream()
                .map(u -> new ProblemView.TimelineEntry(u.getType(), u.getFromStatus(), u.getToStatus(), u.getBody(), u.getCreatedAt()))
                .toList();

        return new ProblemView(p.getId(), p.getPublicCode(), p.getTitle(), p.getDescription(),
                new ProblemView.Category(category.id(), category.code(), category.nameEn(), category.nameHi()),
                new ProblemView.Place(
                        place == null ? null : place.stateName(),
                        place == null ? null : place.cityName(),
                        place == null ? p.getLocalityText() : place.localityName(),
                        p.getAddressText(), p.getLatitude(), p.getLongitude()),
                p.getUrgency(), p.getPeopleAffected(), p.getEstimatedImpact(), p.getDesiredOutcome(),
                p.getAssistanceTypes(), p.getStatus(), p.getStatus().isPublic(), submitter,
                media.describe(p.getMediaIds()),
                new ProblemView.Counts(p.getSupportCount(), p.getSolutionCount(), p.getCommentCount(), p.getFollowerCount()),
                timeline, owner, feed.supports(p.getId(), viewerId), feed.follows(p.getId(), viewerId), p.getCreatedAt());
    }

    private ProblemSummary summary(Problem p) {
        CategoryRef category = taxonomy.findActiveCategory(p.getCategoryId()).orElse(null);
        PlaceRef place = geo.resolve(p.getCityId(), p.getLocalityId(), p.getLocalityText()).orElse(null);
        List<MediaRef> photos = p.getMediaIds().isEmpty() ? List.of() : media.describe(p.getMediaIds().subList(0, 1));
        return new ProblemSummary(p.getId(), p.getPublicCode(), p.getTitle(),
                category == null ? "Other" : category.nameEn(),
                place == null ? p.getLocalityText() : place.localityName(),
                place == null ? null : place.cityName(),
                p.getUrgency(), p.getStatus(), p.getSupportCount(), p.getSolutionCount(),
                photos.isEmpty() ? null : photos.get(0).url(), p.getCreatedAt());
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static NotFoundException notFound() {
        return new NotFoundException("PROBLEM_NOT_FOUND", "This problem does not exist or is not public yet");
    }

    private static ApiException badField(String field, String message) {
        return new FieldException(field, message);
    }
}
