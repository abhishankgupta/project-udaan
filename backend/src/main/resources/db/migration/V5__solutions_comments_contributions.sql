-- solution module: proposals against a problem (section 5.3). Many per problem; the owner accepts one.
CREATE TABLE solutions (
    id                  UUID PRIMARY KEY,
    problem_id          UUID          NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    proposer_id         UUID          NOT NULL REFERENCES users (id),
    title               VARCHAR(150)  NOT NULL,
    summary             VARCHAR(2000) NOT NULL,
    approach            TEXT          NOT NULL,
    estimated_cost_inr  NUMERIC(14, 2),
    timeline_text       VARCHAR(200),
    resources_required  VARCHAR(1000),
    skills_required     VARCHAR(500),
    status              VARCHAR(16)   NOT NULL,
    endorsement_count   INT           NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version             BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX solutions_problem_idx ON solutions (problem_id, created_at);
CREATE INDEX solutions_proposer_idx ON solutions (proposer_id, created_at DESC);
CREATE INDEX solutions_recent_idx ON solutions (status, created_at DESC);
-- At most one accepted solution per problem.
CREATE UNIQUE INDEX solutions_one_accepted_idx ON solutions (problem_id) WHERE status = 'ACCEPTED';

CREATE TABLE solution_endorsements (
    solution_id  UUID        NOT NULL REFERENCES solutions (id) ON DELETE CASCADE,
    user_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (solution_id, user_id)
);

-- discussion module: comments on problems and solutions, one level of replies.
CREATE TABLE comments (
    id           UUID PRIMARY KEY,
    target_type  VARCHAR(16)   NOT NULL,
    target_id    UUID          NOT NULL,
    problem_id   UUID          NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    parent_id    UUID          REFERENCES comments (id) ON DELETE CASCADE,
    author_id    UUID          NOT NULL REFERENCES users (id),
    body         VARCHAR(2000) NOT NULL,
    status       VARCHAR(16)   NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX comments_target_idx ON comments (target_type, target_id, created_at);
CREATE INDEX comments_author_idx ON comments (author_id, created_at DESC);

-- contribution module: offers of help (section 5.4).
CREATE TABLE contributions (
    id             UUID PRIMARY KEY,
    problem_id     UUID          NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    solution_id    UUID          REFERENCES solutions (id) ON DELETE CASCADE,
    user_id        UUID          NOT NULL REFERENCES users (id),
    types          VARCHAR(200)  NOT NULL,
    message        VARCHAR(1000),
    share_contact  BOOLEAN       NOT NULL DEFAULT FALSE,
    status         VARCHAR(16)   NOT NULL,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX contributions_problem_idx ON contributions (problem_id, created_at);
CREATE INDEX contributions_user_idx ON contributions (user_id, created_at DESC);
-- One active offer per person per problem (or per solution).
CREATE UNIQUE INDEX contributions_one_active_idx
    ON contributions (problem_id, user_id, COALESCE(solution_id, '00000000-0000-0000-0000-000000000000'::uuid))
    WHERE status = 'INTERESTED';
