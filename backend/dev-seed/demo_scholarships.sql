-- =====================================================================================================
-- IskolarHub DEVELOPMENT demo data. Every record below is FICTIONAL and marked data_source = 'DEMO'.
-- It is not real scholarship information. Never run this against a production database.
--
-- Run it explicitly (it is NOT a Flyway migration and the application never runs it by itself):
--   docker compose cp backend/dev-seed/demo_scholarships.sql postgres:/tmp/demo_scholarships.sql
--   docker compose exec postgres psql -U iskolarhub -d iskolarhub -v ON_ERROR_STOP=1 -f /tmp/demo_scholarships.sql
--
-- Repeatable: scholarships are upserted by slug and demo rules are replaced, so running it twice leaves
-- exactly the same rows. Records whose data_source is not 'DEMO' are never modified.
-- Deadlines are relative to today (Asia/Manila) so the demo set always has an expired, an urgent
-- (under 7 days), an approaching (7 to 29 days) and an upcoming (30 days or more) scholarship.
--
-- Rule config shapes (validated by the eligibility engine in Phase 3, not by the database):
--   GWA           {"scale":"PH_1_TO_5","comparison":"AT_MOST","value":1.75}
--   FAMILY_INCOME {"currency":"PHP","period":"ANNUAL","max_amount":250000}
--   REGION        {"allowed_region_codes":["NCR","IV_A"]}
--   COURSE        {"allowed_course_codes":["BS_COMPUTER_SCIENCE"]}
--   YEAR_LEVEL    {"min_year":1,"max_year":2}
-- =====================================================================================================

INSERT INTO scholarships (slug, name, provider, description, benefits, info_url, deadline_date, deadline_at, status, data_source)
SELECT v.slug, v.name, v.provider, v.description, v.benefits, v.info_url,
       d.deadline_date,
       CASE WHEN d.deadline_date IS NULL THEN NULL
            ELSE (d.deadline_date + time '23:59:59.999999') AT TIME ZONE 'Asia/Manila' END,
       v.status,
       'DEMO'
FROM (VALUES
    ('demo-academic-excellence', '[DEMO] Academic Excellence Grant', 'Demo Provider A (fictional)',
     'Fictional demo scholarship for high academic standing on the 1.00 to 5.00 scale in years 2 to 4.',
     'Fictional demo benefit: tuition support.', 'https://example.org/demo/academic-excellence', 60, 'PUBLISHED'),
    ('demo-financial-assistance', '[DEMO] Financial Assistance Program', 'Demo Provider B (fictional)',
     'Fictional demo scholarship with a family income ceiling and no grade requirement.',
     'Fictional demo benefit: monthly allowance.', 'https://example.org/demo/financial-assistance', 45, 'PUBLISHED'),
    ('demo-ncr-regional-grant', '[DEMO] Regional Grant (NCR and CALABARZON)', 'Demo Provider C (fictional)',
     'Fictional demo scholarship limited to two regions and an income ceiling.',
     'Fictional demo benefit: book allowance.', 'https://example.org/demo/regional-grant', 20, 'PUBLISHED'),
    ('demo-computing-scholars', '[DEMO] Computing Scholars Award', 'Demo Provider D (fictional)',
     'Fictional demo scholarship for computing courses using a percentage grade, years 3 and 4. Deadline is urgent.',
     'Fictional demo benefit: laptop grant.', 'https://example.org/demo/computing-scholars', 5, 'PUBLISHED'),
    ('demo-first-year-starter', '[DEMO] First-Year Starter Support', 'Demo Provider E (fictional)',
     'Fictional demo scholarship for first-year students with an income ceiling.',
     'Fictional demo benefit: starter kit.', 'https://example.org/demo/first-year-starter', 14, 'PUBLISHED'),
    ('demo-expired-example', '[DEMO] Expired Example Scholarship', 'Demo Provider F (fictional)',
     'Fictional demo scholarship whose deadline has already passed.',
     'Fictional demo benefit: none.', 'https://example.org/demo/expired-example', -10, 'PUBLISHED'),
    ('demo-open-to-all', '[DEMO] Open Rolling Scholarship', 'Demo Provider G (fictional)',
     'Fictional demo scholarship with no configured rules and no deadline.',
     'Fictional demo benefit: one-time award.', 'https://example.org/demo/open-to-all', NULL, 'PUBLISHED'),
    ('demo-draft-hidden', '[DEMO] Unpublished Draft Scholarship', 'Demo Provider H (fictional)',
     'Fictional demo scholarship in DRAFT status that must never be shown to students.',
     'Fictional demo benefit: none.', 'https://example.org/demo/draft-hidden', 30, 'DRAFT')
) AS v(slug, name, provider, description, benefits, info_url, offset_days, status)
CROSS JOIN LATERAL (
    SELECT CASE WHEN v.offset_days IS NULL THEN NULL::date
                ELSE (now() AT TIME ZONE 'Asia/Manila')::date + v.offset_days END AS deadline_date
) AS d
ON CONFLICT (slug) DO UPDATE SET
    name          = EXCLUDED.name,
    provider      = EXCLUDED.provider,
    description   = EXCLUDED.description,
    benefits      = EXCLUDED.benefits,
    info_url      = EXCLUDED.info_url,
    deadline_date = EXCLUDED.deadline_date,
    deadline_at   = EXCLUDED.deadline_at,
    status        = EXCLUDED.status,
    updated_at    = now()
WHERE scholarships.data_source = 'DEMO';

DELETE FROM scholarship_rules
WHERE scholarship_id IN (SELECT id FROM scholarships WHERE slug LIKE 'demo-%' AND data_source = 'DEMO');

INSERT INTO scholarship_rules (scholarship_id, rule_type, config)
SELECT s.id, r.rule_type, r.config::jsonb
FROM (VALUES
    ('demo-academic-excellence', 'GWA',           '{"scale":"PH_1_TO_5","comparison":"AT_MOST","value":1.75}'),
    ('demo-academic-excellence', 'YEAR_LEVEL',    '{"min_year":2,"max_year":4}'),
    ('demo-financial-assistance', 'FAMILY_INCOME', '{"currency":"PHP","period":"ANNUAL","max_amount":250000}'),
    ('demo-ncr-regional-grant',  'REGION',        '{"allowed_region_codes":["NCR","IV_A"]}'),
    ('demo-ncr-regional-grant',  'FAMILY_INCOME', '{"currency":"PHP","period":"ANNUAL","max_amount":400000}'),
    ('demo-computing-scholars',  'COURSE',        '{"allowed_course_codes":["BS_COMPUTER_SCIENCE","BS_INFORMATION_TECHNOLOGY"]}'),
    ('demo-computing-scholars',  'GWA',           '{"scale":"PERCENTAGE","comparison":"AT_LEAST","value":85}'),
    ('demo-computing-scholars',  'YEAR_LEVEL',    '{"min_year":3,"max_year":4}'),
    ('demo-first-year-starter',  'YEAR_LEVEL',    '{"min_year":1,"max_year":1}'),
    ('demo-first-year-starter',  'FAMILY_INCOME', '{"currency":"PHP","period":"ANNUAL","max_amount":350000}'),
    ('demo-expired-example',     'GWA',           '{"scale":"PH_1_TO_5","comparison":"AT_MOST","value":2.5}'),
    ('demo-draft-hidden',        'GWA',           '{"scale":"GPA_4_0","comparison":"AT_LEAST","value":3.5}')
) AS r(slug, rule_type, config)
JOIN scholarships s ON s.slug = r.slug AND s.data_source = 'DEMO';
