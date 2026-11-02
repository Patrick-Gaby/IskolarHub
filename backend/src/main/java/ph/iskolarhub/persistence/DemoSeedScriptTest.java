package ph.iskolarhub.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ph.iskolarhub.scholarship.entity.RuleType;
import ph.iskolarhub.scholarship.entity.Scholarship;
import ph.iskolarhub.scholarship.entity.ScholarshipDataSource;
import ph.iskolarhub.scholarship.entity.ScholarshipRule;
import ph.iskolarhub.scholarship.entity.ScholarshipStatus;
import ph.iskolarhub.scholarship.repository.ScholarshipRepository;
import ph.iskolarhub.scholarship.repository.ScholarshipRuleRepository;
import ph.iskolarhub.support.PostgresDataJpaTest;

/**
 * Runs the real dev-seed script against PostgreSQL. These tests commit (no surrounding transaction) and
 * remove every 'demo-%' row afterwards, so they never leave data behind.
 */
@PostgresDataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DemoSeedScriptTest {

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ScholarshipRepository scholarships;

    @Autowired
    ScholarshipRuleRepository rules;

    @AfterEach
    void removeDemoRows() {
        jdbc.update("delete from scholarships where slug like 'demo-%'");
    }

    private void runSeed() {
        Path script = Path.of("dev-seed/demo_scholarships.sql");
        if (!Files.exists(script)) {
            script = Path.of("backend/dev-seed/demo_scholarships.sql");
        }
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new FileSystemResource(script));
        populator.setSqlScriptEncoding("UTF-8");
        populator.execute(dataSource);
    }

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    @Test
    void seedCreatesTheDemoCatalogAndRules() {
        runSeed();

        assertThat(count("select count(*) from scholarships where slug like 'demo-%'")).isEqualTo(8);
        assertThat(count("select count(*) from scholarships where slug like 'demo-%' and data_source = 'DEMO'")).isEqualTo(8);
        assertThat(count("select count(*) from scholarships where slug like 'demo-%' and status = 'PUBLISHED'")).isEqualTo(7);
        assertThat(count("select count(*) from scholarships where slug like 'demo-%' and status = 'DRAFT'")).isEqualTo(1);
        assertThat(count("select count(*) from scholarships where slug like 'demo-%' and deadline_at is null")).isEqualTo(1);
        assertThat(count("select count(*) from scholarships where slug like 'demo-%' and deadline_at < now()")).isEqualTo(1);
        assertThat(count("select count(*) from scholarship_rules r join scholarships s on s.id = r.scholarship_id "
                + "where s.slug like 'demo-%'")).isEqualTo(12);
    }

    @Test
    void everyDemoRecordIsClearlyLabelledAsFictional() {
        runSeed();
        List<String> names = jdbc.queryForList("select name from scholarships where slug like 'demo-%'", String.class);
        assertThat(names).allMatch(name -> name.startsWith("[DEMO]"));
        List<String> providers = jdbc.queryForList("select provider from scholarships where slug like 'demo-%'", String.class);
        assertThat(providers).allMatch(provider -> provider.contains("fictional"));
    }

    @Test
    void runningTheSeedTwiceDoesNotCreateDuplicatesOrChangeIds() {
        runSeed();
        List<Map<String, Object>> before = jdbc.queryForList("select slug, id from scholarships where slug like 'demo-%' order by slug");

        runSeed();
        List<Map<String, Object>> after = jdbc.queryForList("select slug, id from scholarships where slug like 'demo-%' order by slug");

        assertThat(after).isEqualTo(before);
        assertThat(count("select count(*) from scholarships where slug like 'demo-%'")).isEqualTo(8);
        assertThat(count("select count(*) from scholarship_rules r join scholarships s on s.id = r.scholarship_id "
                + "where s.slug like 'demo-%'")).isEqualTo(12);
    }

    @Test
    void seedNeverOverwritesAVerifiedRecordThatUsesADemoSlug() {
        jdbc.update("insert into scholarships (slug, name, provider, status, data_source) "
                + "values ('demo-academic-excellence', 'Real Record', 'Real Provider', 'PUBLISHED', 'VERIFIED')");

        runSeed();

        assertThat(jdbc.queryForObject("select name from scholarships where slug = 'demo-academic-excellence'", String.class))
                .isEqualTo("Real Record");
        assertThat(jdbc.queryForObject("select data_source from scholarships where slug = 'demo-academic-excellence'", String.class))
                .isEqualTo("VERIFIED");
        assertThat(count("select count(*) from scholarship_rules r join scholarships s on s.id = r.scholarship_id "
                + "where s.slug = 'demo-academic-excellence'")).isZero();
    }

    @Test
    void seededDeadlinesAreEndOfDayInManilaAndHibernateCanReadTheData() {
        runSeed();

        List<String> publishedOrder = scholarships.findByStatusOrderByDeadlineAtAscNameAsc(ScholarshipStatus.PUBLISHED).stream()
                .map(Scholarship::getSlug)
                .filter(slug -> slug.startsWith("demo-"))
                .toList();
        assertThat(publishedOrder).containsExactly(
                "demo-expired-example", "demo-computing-scholars", "demo-first-year-starter", "demo-ncr-regional-grant",
                "demo-financial-assistance", "demo-academic-excellence", "demo-open-to-all");

        Scholarship academic = scholarships.findBySlug("demo-academic-excellence").orElseThrow();
        assertThat(academic.getDataSource()).isEqualTo(ScholarshipDataSource.DEMO);
        assertThat(academic.getDeadlineDate()).isNotNull();
        assertThat(academic.getDeadlineAt().getNano()).isEqualTo(999_999_000);

        List<ScholarshipRule> academicRules = rules.findByScholarshipId(academic.getId());
        assertThat(academicRules).extracting(ScholarshipRule::getRuleType)
                .containsExactlyInAnyOrder(RuleType.GWA, RuleType.YEAR_LEVEL);
        ScholarshipRule gwa = academicRules.stream().filter(r -> r.getRuleType() == RuleType.GWA).findFirst().orElseThrow();
        assertThat(gwa.getConfig().get("scale")).isEqualTo("PH_1_TO_5");
    }
}
