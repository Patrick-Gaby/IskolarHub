-- Durable notification queue. Rows are written in the same transaction as the change that caused them.
-- status: PENDING (waiting, scheduled_at is the earliest processing time and moves forward on retry),
--         PROCESSING (claimed; locked_at set, so stale claims can be recovered after a restart),
--         SENT (accepted by the mail server; sent_at set), FAILED (gave up after the maximum attempts).
-- last_error must hold a short sanitized reason only (never message bodies, tokens or addresses).
CREATE TABLE notification_events (
                                     id              uuid         NOT NULL DEFAULT gen_random_uuid(),
                                     user_id         uuid         NOT NULL,
                                     scholarship_id  uuid,
                                     event_type      varchar(40)  NOT NULL,
                                     idempotency_key varchar(200) NOT NULL,
                                     scheduled_at    timestamptz  NOT NULL,
                                     status          varchar(20)  NOT NULL DEFAULT 'PENDING',
                                     attempt_count   integer      NOT NULL DEFAULT 0,
                                     last_attempt_at timestamptz,
                                     locked_at       timestamptz,
                                     sent_at         timestamptz,
                                     last_error      varchar(500),
                                     created_at      timestamptz  NOT NULL DEFAULT now(),
                                     updated_at      timestamptz  NOT NULL DEFAULT now(),
                                     CONSTRAINT pk_notification_events PRIMARY KEY (id),
                                     CONSTRAINT uq_notification_events_idempotency_key UNIQUE (idempotency_key),
                                     CONSTRAINT fk_notification_events_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                                     CONSTRAINT fk_notification_events_scholarship FOREIGN KEY (scholarship_id) REFERENCES scholarships (id) ON DELETE RESTRICT,
                                     CONSTRAINT ck_notification_events_type CHECK (event_type IN ('SUBSCRIPTION_CONFIRMATION', 'DEADLINE_REMINDER')),
                                     CONSTRAINT ck_notification_events_status CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED')),
                                     CONSTRAINT ck_notification_events_attempts CHECK (attempt_count >= 0),
                                     CONSTRAINT ck_notification_events_key CHECK (length(idempotency_key) > 0),
                                     CONSTRAINT ck_notification_events_sent_state CHECK ((status = 'SENT') = (sent_at IS NOT NULL)),
                                     CONSTRAINT ck_notification_events_lock_state CHECK ((status = 'PROCESSING') = (locked_at IS NOT NULL)),
                                     CONSTRAINT ck_notification_events_reminder_scholarship CHECK (event_type <> 'DEADLINE_REMINDER' OR scholarship_id IS NOT NULL)
);

-- Due-event scan: pending events whose scheduled time has arrived.
CREATE INDEX ix_notification_events_due ON notification_events (scheduled_at) WHERE status = 'PENDING';
-- Recovery scan: claims that were never completed (for example the app stopped mid-send).
CREATE INDEX ix_notification_events_stale_lock ON notification_events (locked_at) WHERE status = 'PROCESSING';