-- A user's notification subscription to a scholarship. Deleting a user removes their subscriptions;
-- a scholarship that still has subscriptions cannot be deleted (archive it instead).
CREATE TABLE subscriptions (
                               id             uuid        NOT NULL DEFAULT gen_random_uuid(),
                               user_id        uuid        NOT NULL,
                               scholarship_id uuid        NOT NULL,
                               status         varchar(20) NOT NULL DEFAULT 'ACTIVE',
                               created_at     timestamptz NOT NULL DEFAULT now(),
                               updated_at     timestamptz NOT NULL DEFAULT now(),
                               CONSTRAINT pk_subscriptions PRIMARY KEY (id),
                               CONSTRAINT uq_subscriptions_user_scholarship UNIQUE (user_id, scholarship_id),
                               CONSTRAINT fk_subscriptions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                               CONSTRAINT fk_subscriptions_scholarship FOREIGN KEY (scholarship_id) REFERENCES scholarships (id) ON DELETE RESTRICT,
                               CONSTRAINT ck_subscriptions_status CHECK (status IN ('ACTIVE', 'UNSUBSCRIBED'))
);

-- Finding the active subscribers of a scholarship (deadline reminder scan).
CREATE INDEX ix_subscriptions_scholarship_status ON subscriptions (scholarship_id, status);