package ph.iskolarhub.scholarship.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static ph.iskolarhub.support.ConstraintAssertions.assertViolates;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import ph.iskolarhub.scholarship.entity.RuleType;
import ph.iskolarhub.scholarship.entity.Scholarship;
import ph.iskolarhub.scholarship.entity.ScholarshipDataSource;
import ph.iskolarhub.scholarship.entity.ScholarshipRule;
import ph.iskolarhub.scholarship.entity.ScholarshipStatus;
import ph.iskolarhub.support.PostgresDataJpaTest;
import ph.iskolarhub.support.TestData;

@PostgresDataJpaTest
class ScholarshipRepositoryTest {

    @Autowired
    ScholarshipRepository scholarships;

    @Autowired
    ScholarshipRuleRepository rules;

    @Autowired
    TestEntityManager em;

    @Autowired
    JdbcTemplate jdbc;

    private Scholarship saved(String slug) {
        return scholarships.saveAndFlush(TestData.newScholarship(slug));
    }

    @Test
    void scholarshipWithDateOnlyDeadlineRoundTripsExactly() {
        Scholarship s = TestData.newScholarship("round-trip");
        s.updateDetails("Round trip", "Provider", "Description", "Benefits", "https://example.org/x");
        s.useDateOnlyDeadline(LocalDate.of(2026, 12, 31));
        s.publish();
        scholarships.saveAndFlush(s);
        em.clear();

        Scholarship loaded = scholarships.findBySlug("round-trip").orElseThrow();

        assertThat(loaded.getDeadlineDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(loaded.getDeadlineAt()).isEqualTo(Instant.parse("2026-12-31T15:59:59.999999Z"));
        assertThat(loaded.getStatus()).isEqualTo(ScholarshipStatus.PUBLISHED);
        assertThat(loaded.getDataSource()).isEqualTo(ScholarshipDataSource.DEMO);
        assertThat(loaded.getInfoUrl()).isEqualTo("https://example.org/x");
        assertThat(loaded.getDescription()).isEqualTo("Description");
    }

    @Test
    void exactDeadlineIsStoredWithoutACalendarDate() {
        Scholarship s = TestData.newScholarship("exact-deadline");
        s.useExactDeadline(Instant.parse("2030-03-03T09:30:00Z"));
        scholarships.saveAndFlush(s);
        em.clear();

        Scholarship loaded = scholarships.findBySlug("exact-deadline").orElseThrow();
        assertThat(loaded.getDeadlineDate()).isNull();
        assertThat(loaded.getDeadlineAt()).isEqualTo(Instant.parse("2030-03-03T09:30:00Z"));
    }

    @Test
    void publishedScholarshipsAreOrderedBySoonestDeadlineWithNoDeadlineLast() {
        Scholarship later = TestData.newScholarship("order-test-later");
        later.useDateOnlyDeadline(LocalDate.of(2030, 1, 3));
        later.publish();
        Scholarship none = TestData.newScholarship("order-test-none");
        none.publish();
        Scholarship soon = TestData.newScholarship("order-test-soon");
        soon.useDateOnlyDeadline(LocalDate.of(2030, 1, 2));
        soon.publish();
        Scholarship draft = TestData.newScholarship("order-test-draft");
        draft.useDateOnlyDeadline(LocalDate.of(2030, 1, 1));
        scholarships.saveAllAndFlush(List.of(later, none, soon, draft));
        em.clear();

        List<String> slugs = scholarships.findByStatusOrderByDeadlineAtAscNameAsc(ScholarshipStatus.PUBLISHED).stream()
                .map(Scholarship::getSlug)
                .filter(slug -> slug.startsWith("order-test-"))
                .toList();

        assertThat(slugs).containsExactly("order-test-soon", "order-test-later", "order-test-none");
    }

    @Test
    void slugMustBeUnique() {
        saved("unique-slug");
        assertViolates("uq_scholarships_slug", () -> scholarships.saveAndFlush(TestData.newScholarship("unique-slug")));
    }

    @Test
    void ruleConfigRoundTripsAsJson() {
        Scholarship s = saved("json-rules");
        rules.save(new ScholarshipRule(s, RuleType.GWA,
                Map.of("scale", "PH_1_TO_5", "comparison", "AT_MOST", "value", 1.75)));
        rules.save(new ScholarshipRule(s, RuleType.REGION,
                Map.of("allowed_region_codes", List.of("NCR", "IV_A"))));
        rules.flush();
        em.clear();

        List<ScholarshipRule> loaded = rules.findByScholarshipId(s.getId());

        assertThat(loaded).extracting(ScholarshipRule::getRuleType).containsExactlyInAnyOrder(RuleType.GWA, RuleType.REGION);
        Map<String, Object> gwa = loaded.stream().filter(r -> r.getRuleType() == RuleType.GWA).findFirst().orElseThrow().getConfig();
        assertThat(gwa.get("scale")).isEqualTo("PH_1_TO_5");
        assertThat(gwa.get("comparison")).isEqualTo("AT_MOST");
        assertThat(((Number) gwa.get("value")).doubleValue()).isEqualTo(1.75);
        Map<String, Object> region = loaded.stream().filter(r -> r.getRuleType() == RuleType.REGION).findFirst().orElseThrow().getConfig();
        assertThat((List<String>) region.get("allowed_region_codes"))
                .containsExactly("NCR", "IV_A");
    }

    @Test
    void rulesOfSeveralScholarshipsLoadInOneQuery() {
        Scholarship a = saved("bulk-a");
        Scholarship b = saved("bulk-b");
        saved("bulk-c");
        rules.save(new ScholarshipRule(a, RuleType.YEAR_LEVEL, Map.of("min_year", 1, "max_year", 2)));
        rules.save(new ScholarshipRule(b, RuleType.COURSE, Map.of("allowed_course_codes", List.of("BS_NURSING"))));
        rules.flush();
        em.clear();

        List<ScholarshipRule> loaded = rules.findByScholarshipIdIn(List.of(a.getId(), b.getId()));

        assertThat(loaded).hasSize(2);
    }

    @Test
    void sameRuleTypeCannotBeAddedTwiceToOneScholarship() {
        Scholarship s = saved("dup-rule");
        rules.saveAndFlush(new ScholarshipRule(s, RuleType.GWA, Map.of("scale", "PERCENTAGE", "comparison", "AT_LEAST", "value", 85)));
        assertViolates("uq_scholarship_rules_scholarship_type", () -> rules.saveAndFlush(
                new ScholarshipRule(s, RuleType.GWA, Map.of("scale", "PERCENTAGE", "comparison", "AT_LEAST", "value", 90))));
    }

    @Test
    void deletingAScholarshipDeletesItsRules() {
        Scholarship s = saved("cascade-rules");
        rules.saveAndFlush(new ScholarshipRule(s, RuleType.FAMILY_INCOME, Map.of("max_amount", 250000)));
        em.clear();

        jdbc.update("delete from scholarships where id = ?", s.getId());

        Integer left = jdbc.queryForObject(
                "select count(*) from scholarship_rules where scholarship_id = ?", Integer.class, s.getId());
        assertThat(left).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "[]", "\"text\"", "1"})
    void databaseRejectsRuleConfigThatIsNotANonEmptyJsonObject(String json) {
        Scholarship s = saved("bad-config-" + Math.abs(json.hashCode()));
        assertViolates("ck_scholarship_rules_config", () -> jdbc.update(
                "insert into scholarship_rules (scholarship_id, rule_type, config) values (?, 'GWA', cast(? as jsonb))",
                s.getId(), json));
    }

    @Test
    void databaseRejectsAnUnknownRuleType() {
        Scholarship s = saved("bad-rule-type");
        assertViolates("ck_scholarship_rules_type", () -> jdbc.update(
                "insert into scholarship_rules (scholarship_id, rule_type, config) values (?, 'MAGIC', cast('{\"a\":1}' as jsonb))",
                s.getId()));
    }

    @Test
    void databaseRejectsADeadlineDateThatDisagreesWithTheInstantInManila() {
        // 2026-12-31T16:00:00Z is already 1 January 2027 in Manila.
        assertViolates("ck_scholarships_deadline_consistent", () -> jdbc.update(
                "insert into scholarships (slug, name, provider, status, data_source, deadline_date, deadline_at) "
                        + "values ('inconsistent', 'n', 'p', 'DRAFT', 'DEMO', ?, ?)",
                LocalDate.of(2026, 12, 31), OffsetDateTime.parse("2026-12-31T16:00:00Z")));
    }

    @Test
    void databaseRejectsADeadlineDateWithoutAnInstant() {
        assertViolates("ck_scholarships_deadline_consistent", () -> jdbc.update(
                "insert into scholarships (slug, name, provider, status, data_source, deadline_date) "
                        + "values ('date-only-row', 'n', 'p', 'DRAFT', 'DEMO', ?)",
                LocalDate.of(2026, 12, 31)));
    }

    @Test
    void databaseRejectsAMalformedInfoUrl() {
        assertViolates("ck_scholarships_info_url", () -> jdbc.update(
                "insert into scholarships (slug, name, provider, status, data_source, info_url) "
                        + "values ('bad-url', 'n', 'p', 'DRAFT', 'DEMO', 'javascript:alert(1)')"));
    }

    @Test
    void databaseRejectsAnInvalidSlug() {
        assertViolates("ck_scholarships_slug", () -> jdbc.update(
                "insert into scholarships (slug, name, provider, status, data_source) "
                        + "values ('Bad Slug', 'n', 'p', 'DRAFT', 'DEMO')"));
    }

    @Test
    void databaseRejectsAnUnknownStatus() {
        assertViolates("ck_scholarships_status", () -> jdbc.update(
                "insert into scholarships (slug, name, provider, status, data_source) "
                        + "values ('bad-status', 'n', 'p', 'LIVE', 'DEMO')"));
    }

    @Test
    void databaseRejectsAnUnknownDataSource() {
        assertViolates("ck_scholarships_data_source", () -> jdbc.update(
                "insert into scholarships (slug, name, provider, status, data_source) "
                        + "values ('bad-source', 'n', 'p', 'DRAFT', 'REAL')"));
    }
}
