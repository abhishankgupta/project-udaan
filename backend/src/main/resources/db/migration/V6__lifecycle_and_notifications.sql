-- Who can drive the problem forward after a solution is accepted, and when it was resolved.
ALTER TABLE problems ADD COLUMN accepted_solution_id UUID REFERENCES solutions (id) ON DELETE SET NULL;
ALTER TABLE problems ADD COLUMN accepted_proposer_id UUID REFERENCES users (id);
ALTER TABLE problems ADD COLUMN resolved_at TIMESTAMPTZ;

-- Photos attached to timeline entries (progress updates and resolution evidence).
ALTER TABLE problem_updates ADD COLUMN author_role VARCHAR(24);
CREATE TABLE problem_update_media (
    update_id   UUID NOT NULL REFERENCES problem_updates (id) ON DELETE CASCADE,
    media_id    UUID NOT NULL REFERENCES media_objects (id),
    sort_order  INT  NOT NULL,
    PRIMARY KEY (update_id, sort_order)
);

-- Community confirmation of a resolution (section 6 of the architecture doc).
CREATE TABLE problem_resolution_votes (
    problem_id  UUID        NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    vote        VARCHAR(8)  NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (problem_id, user_id)
);

-- notification module: in-app notifications.
CREATE TABLE notifications (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type        VARCHAR(40)  NOT NULL,
    title       VARCHAR(200) NOT NULL,
    body        VARCHAR(500),
    link        VARCHAR(200) NOT NULL,
    actor_id    UUID,
    problem_id  UUID,
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX notifications_user_idx ON notifications (user_id, created_at DESC);
CREATE INDEX notifications_unread_idx ON notifications (user_id) WHERE read_at IS NULL;
