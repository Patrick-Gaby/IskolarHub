package ph.iskolarhub.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import ph.iskolarhub.support.PostgresTestConfig;

/** Needs Docker. Skipped (not failed) when no Docker daemon is available. */
@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestConfig.class)
@Testcontainers(disabledWithoutDocker = true)
class DatabaseMigrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void contextStartsAndBaselineMigrationIsApplied() {
        Integer applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where version = '1' and success", Integer.class);
        assertThat(applied).isEqualTo(1);
    }

    @Test
    void databaseIsPostgresSixteen() {
        String version = jdbc.queryForObject("show server_version_num", String.class);
        assertThat(version).startsWith("16");
    }
}
