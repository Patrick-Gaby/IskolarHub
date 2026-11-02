-- Accounts. Email is stored normalized (trimmed, lower-case) by the application; the CHECK enforces it.
CREATE TABLE users (
                       id            uuid         NOT NULL DEFAULT gen_random_uuid(),
                       email         varchar(254) NOT NULL,
                       password_hash varchar(255) NOT NULL,
                       status        varchar(20)  NOT NULL DEFAULT 'ACTIVE',
                       created_at    timestamptz  NOT NULL DEFAULT now(),
                       updated_at    timestamptz  NOT NULL DEFAULT now(),
                       CONSTRAINT pk_users PRIMARY KEY (id),
                       CONSTRAINT uq_users_email UNIQUE (email),
                       CONSTRAINT ck_users_email_normalized CHECK (position('@' IN email) > 1 AND email = lower(btrim(email))),
                       CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);
