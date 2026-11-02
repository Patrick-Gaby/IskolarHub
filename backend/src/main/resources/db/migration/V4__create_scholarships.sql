-- Curated scholarship catalog. data_source separates DEMO records from VERIFIED real information.
-- Deadline policy: deadline_at is the exact instant. For a date-only deadline, deadline_date holds the
-- calendar date and deadline_at is the END of that date in Asia/Manila; the CHECK keeps the two consistent.
CREATE TABLE scholarships (
                              id            uuid          NOT NULL DEFAULT gen_random_uuid(),
                              slug          varchar(100)  NOT NULL,
                              name          varchar(200)  NOT NULL,
                              provider      varchar(200)  NOT NULL,
                              description   text,
                              benefits      text,
                              info_url      varchar(2048),
                              deadline_date date,
                              deadline_at   timestamptz,
                              status        varchar(20)   NOT NULL DEFAULT 'DRAFT',
                              data_source   varchar(20)   NOT NULL,
                              created_at    timestamptz   NOT NULL DEFAULT now(),
                              updated_at    timestamptz   NOT NULL DEFAULT now(),
                              CONSTRAINT pk_scholarships PRIMARY KEY (id),
                              CONSTRAINT uq_scholarships_slug UNIQUE (slug),
                              CONSTRAINT ck_scholarships_slug CHECK (slug ~ '^[a-z0-9]+(-[a-z0-9]+)*$'),
    CONSTRAINT ck_scholarships_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_scholarships_data_source CHECK (data_source IN ('DEMO', 'VERIFIED')),
    CONSTRAINT ck_scholarships_info_url CHECK (info_url IS NULL OR info_url ~ '^https?://[^[:space:]]+$'),
    CONSTRAINT ck_scholarships_deadline_consistent CHECK (
        deadline_date IS NULL
        OR (deadline_at IS NOT NULL AND (deadline_at AT TIME ZONE 'Asia/Manila')::date = deadline_date)
    )
);

-- Listing published scholarships ordered by deadline (nulls last) is the main read path.
CREATE INDEX ix_scholarships_status_deadline ON scholarships (status, deadline_at);