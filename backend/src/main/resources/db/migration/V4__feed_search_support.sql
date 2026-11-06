-- Supports (upvotes) and follows: one row per user per problem.
CREATE TABLE problem_supports (
    problem_id  UUID        NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (problem_id, user_id)
);
CREATE INDEX problem_supports_user_idx ON problem_supports (user_id, created_at DESC);

CREATE TABLE problem_follows (
    problem_id  UUID        NOT NULL REFERENCES problems (id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (problem_id, user_id)
);
CREATE INDEX problem_follows_user_idx ON problem_follows (user_id, created_at DESC);

-- Full-text search (section 9). The 'simple' configuration does no stemming, which suits
-- content that mixes English, Hindi and Hinglish.
ALTER TABLE problems ADD COLUMN search_vector tsvector GENERATED ALWAYS AS (
    setweight(to_tsvector('simple', coalesce(title, '')), 'A') ||
    setweight(to_tsvector('simple', coalesce(description, '')), 'B')
) STORED;
CREATE INDEX problems_search_idx ON problems USING gin (search_vector);
CREATE INDEX problems_title_trgm_idx ON problems USING gin (title gin_trgm_ops);
CREATE INDEX problems_status_created_idx ON problems (status, created_at DESC);
