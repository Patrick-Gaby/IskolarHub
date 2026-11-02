package ph.iskolarhub.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ph.iskolarhub.support.PostgresDataJpaTest;

/**
 * Verifies the schema produced by Flyway on a fresh PostgreSQL 16 and that Hibernate mapped the six entities
 * (the application context only starts if Hibernate's schema validation succeeded).
 */
@PostgresDataJpaTest
class DatabaseSchemaTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManager entityManager;

    @Test
    void flywayAppliedMigrationsOneToSevenSuccessfully() {
        List<String> applied = jdbc.queryForList(
                "select version from flyway_schema_history where success order by installed_rank", String.class);
        assertThat(applied).contains("1", "2", "3", "4", "5", "6", "7");
        Integer failed = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where not success", Integer.class);
        assertThat(failed).isZero();
    }

    @Test
    void allSixDomainTablesExist() {
        List<String> tables = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public'", String.class);
        assertThat(tables).contains(
                "users", "student_profiles", "scholarships", "scholarship_rules", "subscriptions", "notification_events");
    }

    @Test
    void everyDeclaredConstraintExists() {
        List<String> constraints = jdbc.queryForList(
                "select conname from pg_constraint where connamespace = 'public'::regnamespace", String.class);
        assertThat(constraints).contains(
                "pk_users", "uq_users_email", "ck_users_email_normalized", "ck_users_status",
                "pk_student_profiles", "uq_student_profiles_user", "fk_student_profiles_user",
                "ck_student_profiles_gwa_pair", "ck_student_profiles_gwa_scale", "ck_student_profiles_gwa_range",
                "ck_student_profiles_income", "ck_student_profiles_region", "ck_student_profiles_course",
                "ck_student_profiles_year",
                "pk_scholarships", "uq_scholarships_slug", "ck_scholarships_slug", "ck_scholarships_status",
                "ck_scholarships_data_source", "ck_scholarships_info_url", "ck_scholarships_deadline_consistent",
                "pk_scholarship_rules", "uq_scholarship_rules_scholarship_type", "fk_scholarship_rules_scholarship",
                "ck_scholarship_rules_type", "ck_scholarship_rules_config",
                "pk_subscriptions", "uq_subscriptions_user_scholarship", "fk_subscriptions_user",
                "fk_subscriptions_scholarship", "ck_subscriptions_status",
                "pk_notification_events", "uq_notification_events_idempotency_key", "fk_notification_events_user",
                "fk_notification_events_scholarship", "ck_notification_events_type", "ck_notification_events_status",
                "ck_notification_events_attempts", "ck_notification_events_key", "ck_notification_events_sent_state",
                "ck_notification_events_lock_state", "ck_notification_events_reminder_scholarship");
    }

    @Test
    void declaredIndexesExist() {
        List<String> indexes = jdbc.queryForList(
                "select indexname from pg_indexes where schemaname = 'public'", String.class);
        assertThat(indexes).contains(
                "ix_scholarships_status_deadline", "ix_subscriptions_scholarship_status",
                "ix_notification_events_due", "ix_notification_events_stale_lock");
    }

    @Test
    void everyTimestampColumnIsTimestamptz() {
        Integer wrong = jdbc.queryForObject(
                "select count(*) from information_schema.columns where table_schema = 'public' "
                        + "and table_name <> 'flyway_schema_history' and column_name ~ '_at$' "
                        + "and data_type <> 'timestamp with time zone'", Integer.class);
        assertThat(wrong).isZero();
    }

    @Test
    void everyIdColumnIsUuid() {
        Integer wrong = jdbc.queryForObject(
                "select count(*) from information_schema.columns where table_schema = 'public' "
                        + "and column_name = 'id' and data_type <> 'uuid'", Integer.class);
        assertThat(wrong).isZero();
    }

    @Test
    void hibernateMapsAllSixEntities() {
        List<String> entities = entityManager.getMetamodel().getEntities().stream().map(EntityType::getName).toList();
        assertThat(entities).contains(
                "User", "StudentProfile", "Scholarship", "ScholarshipRule", "Subscription", "NotificationEvent");
    }
}
