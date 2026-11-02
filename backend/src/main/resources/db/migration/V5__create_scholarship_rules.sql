-- One configured requirement per rule type per scholarship (all rules combine with AND in version one).
-- config is validated by the eligibility engine (Phase 3); the database only guarantees a non-empty JSON object.
CREATE TABLE scholarship_rules (
                                   id             uuid        NOT NULL DEFAULT gen_random_uuid(),
                                   scholarship_id uuid        NOT NULL,
                                   rule_type      varchar(20) NOT NULL,
                                   config         jsonb       NOT NULL,
                                   created_at     timestamptz NOT NULL DEFAULT now(),
                                   updated_at     timestamptz NOT NULL DEFAULT now(),
                                   CONSTRAINT pk_scholarship_rules PRIMARY KEY (id),
                                   CONSTRAINT uq_scholarship_rules_scholarship_type UNIQUE (scholarship_id, rule_type),
                                   CONSTRAINT fk_scholarship_rules_scholarship FOREIGN KEY (scholarship_id) REFERENCES scholarships (id) ON DELETE CASCADE,
                                   CONSTRAINT ck_scholarship_rules_type CHECK (rule_type IN ('GWA', 'FAMILY_INCOME', 'REGION', 'COURSE', 'YEAR_LEVEL')),
                                   CONSTRAINT ck_scholarship_rules_config CHECK (jsonb_typeof(config) = 'object' AND config <> '{}'::jsonb)
    );