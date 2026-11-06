-- identity module: accounts, roles, refresh tokens, one-time phone codes.
CREATE TABLE users (
    id                 UUID PRIMARY KEY,
    email              VARCHAR(254) UNIQUE,
    phone_e164         VARCHAR(16)  UNIQUE,
    password_hash      VARCHAR(100),
    display_name       VARCHAR(80)  NOT NULL,
    status             VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    email_verified_at  TIMESTAMPTZ,
    phone_verified_at  TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT users_has_login CHECK (email IS NOT NULL OR phone_e164 IS NOT NULL)
);

CREATE TABLE user_roles (
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role        VARCHAR(32) NOT NULL,
    granted_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role)
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,
    family_id   UUID         NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX refresh_tokens_family_idx ON refresh_tokens (family_id);

-- Codes live in Postgres rather than Redis (no Redis in the free setup); hashed, short-lived.
CREATE TABLE otp_codes (
    id           UUID PRIMARY KEY,
    phone_e164   VARCHAR(16) NOT NULL,
    code_hash    VARCHAR(64) NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL,
    attempts     INT         NOT NULL DEFAULT 0,
    consumed_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX otp_codes_phone_idx ON otp_codes (phone_e164, created_at DESC);

-- profile module: public profile details. Contact fields stay in users and are never public.
CREATE TABLE user_profiles (
    user_id     UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    bio         VARCHAR(500),
    city        VARCHAR(80),
    locality    VARCHAR(120),
    skills      VARCHAR(500),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
