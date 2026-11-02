package ph.iskolarhub.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ph.iskolarhub.support.ConstraintAssertions.assertViolates;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import ph.iskolarhub.auth.entity.User;
import ph.iskolarhub.auth.repository.UserRepository;
import ph.iskolarhub.notification.entity.NotificationEvent;
import ph.iskolarhub.notification.entity.NotificationEventStatus;
import ph.iskolarhub.notification.entity.NotificationEventType;
import ph.iskolarhub.scholarship.entity.Scholarship;
import ph.iskolarhub.scholarship.repository.ScholarshipRepository;
import ph.iskolarhub.support.PostgresDataJpaTest;
import ph.iskolarhub.support.TestData;

@PostgresDataJpaTest
class NotificationEventRepositoryTest {

    private static final Instant WHEN = Instant.parse("2030-01-01T00:00:00Z");
    private static final String INSERT =
            "insert into notification_events (user_id, scholarship_id, event_type, idempotency_key, scheduled_at, "
                    + "status, attempt_count, locked_at, sent_at) "
                    + "values (?, ?, ?, ?, now(), ?, ?, cast(? as timestamptz), cast(? as timestamptz))";

    @Autowired
    NotificationEventRepository events;

    @Autowired
    UserRepository users;

    @Autowired
    ScholarshipRepository scholarships;

    @Autowired
    TestEntityManager em;

    @Autowired
    JdbcTemplate jdbc;

    private User user;
    private Scholarship scholarship;

    private void givenUserAndScholarship() {
        user = users.saveAndFlush(TestData.newUser("events@example.com"));
        scholarship = scholarships.saveAndFlush(TestData.newScholarship("event-target"));
    }

    private int insertRaw(String key, String type, UUID scholarshipId, String status, int attempts, String lockedAt, String sentAt) {
        return jdbc.update(INSERT, user.getId(), scholarshipId, type, key, status, attempts, lockedAt, sentAt);
    }

    @Test
    void newEventStartsPendingWithZeroAttemptsAndNoTimestamps() {
        givenUserAndScholarship();
        events.saveAndFlush(new NotificationEvent(
                user, scholarship, NotificationEventType.DEADLINE_REMINDER, WHEN, "reminder:test:1"));
        em.clear();

        NotificationEvent loaded = events.findByIdempotencyKey("reminder:test:1").orElseThrow();

        assertThat(loaded.getStatus()).isEqualTo(NotificationEventStatus.PENDING);
        assertThat(loaded.getAttemptCount()).isZero();
        assertThat(loaded.getScheduledAt()).isEqualTo(WHEN);
        assertThat(loaded.getLastAttemptAt()).isNull();
        assertThat(loaded.getLockedAt()).isNull();
        assertThat(loaded.getSentAt()).isNull();
        assertThat(loaded.getLastError()).isNull();
        assertThat(loaded.getEventType()).isEqualTo(NotificationEventType.DEADLINE_REMINDER);
        assertThat(loaded.getScholarship().getId()).isEqualTo(scholarship.getId());
        assertThat(events.existsByIdempotencyKey("reminder:test:1")).isTrue();
        assertThat(events.existsByIdempotencyKey("reminder:test:2")).isFalse();
    }

    @Test
    void confirmationEventDoesNotNeedAScholarship() {
        givenUserAndScholarship();
        events.saveAndFlush(new NotificationEvent(user, null, NotificationEventType.SUBSCRIPTION_CONFIRMATION, WHEN, "confirm:test:1"));
        em.clear();
        assertThat(events.findByIdempotencyKey("confirm:test:1").orElseThrow().getScholarship()).isNull();
    }

    @Test
    void aReminderWithoutAScholarshipIsRejectedByTheEntity() {
        givenUserAndScholarship();
        assertThatThrownBy(() -> new NotificationEvent(user, null, NotificationEventType.DEADLINE_REMINDER, WHEN, "k"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theSameIdempotencyKeyCannotBeStoredTwice() {
        givenUserAndScholarship();
        events.saveAndFlush(new NotificationEvent(user, scholarship, NotificationEventType.DEADLINE_REMINDER, WHEN, "dup-key"));
        assertViolates("uq_notification_events_idempotency_key", () -> events.saveAndFlush(
                new NotificationEvent(user, scholarship, NotificationEventType.DEADLINE_REMINDER, WHEN, "dup-key")));
    }

    @Test
    void everyValidStateCombinationIsAccepted() {
        givenUserAndScholarship();
        UUID sid = scholarship.getId();
        insertRaw("ok-pending", "DEADLINE_REMINDER", sid, "PENDING", 0, null, null);
        insertRaw("ok-processing", "DEADLINE_REMINDER", sid, "PROCESSING", 1, "2030-01-01T00:00:00Z", null);
        insertRaw("ok-sent", "DEADLINE_REMINDER", sid, "SENT", 1, null, "2030-01-01T00:00:00Z");
        insertRaw("ok-failed", "DEADLINE_REMINDER", sid, "FAILED", 5, null, null);
        Integer count = jdbc.queryForObject(
                "select count(*) from notification_events where idempotency_key like 'ok-%'", Integer.class);
        assertThat(count).isEqualTo(4);
    }

    @ParameterizedTest
    @CsvSource(nullValues = "NULL", value = {
            "SENT,NULL,NULL,ck_notification_events_sent_state",
            "PENDING,NULL,2030-01-01T00:00:00Z,ck_notification_events_sent_state",
            "FAILED,NULL,2030-01-01T00:00:00Z,ck_notification_events_sent_state",
            "PROCESSING,NULL,NULL,ck_notification_events_lock_state",
            "PENDING,2030-01-01T00:00:00Z,NULL,ck_notification_events_lock_state",
            "SENT,2030-01-01T00:00:00Z,2030-01-01T00:00:00Z,ck_notification_events_lock_state"
    })
    void databaseRejectsInconsistentStatusAndTimestamps(String status, String lockedAt, String sentAt, String constraint) {
        givenUserAndScholarship();
        assertViolates(constraint, () -> insertRaw(
                "bad-state", "DEADLINE_REMINDER", scholarship.getId(), status, 0, lockedAt, sentAt));
    }

    @Test
    void databaseRejectsANegativeAttemptCount() {
        givenUserAndScholarship();
        assertViolates("ck_notification_events_attempts", () -> insertRaw(
                "negative", "DEADLINE_REMINDER", scholarship.getId(), "PENDING", -1, null, null));
    }

    @Test
    void databaseRejectsAnUnknownStatus() {
        givenUserAndScholarship();
        assertViolates("ck_notification_events_status", () -> insertRaw(
                "unknown-status", "DEADLINE_REMINDER", scholarship.getId(), "RETRYING", 0, null, null));
    }

    @Test
    void databaseRejectsAnUnknownEventType() {
        givenUserAndScholarship();
        assertViolates("ck_notification_events_type", () -> insertRaw(
                "unknown-type", "NEWSLETTER", scholarship.getId(), "PENDING", 0, null, null));
    }

    @Test
    void databaseRejectsAReminderWithoutAScholarship() {
        givenUserAndScholarship();
        assertViolates("ck_notification_events_reminder_scholarship", () -> insertRaw(
                "reminder-no-scholarship", "DEADLINE_REMINDER", null, "PENDING", 0, null, null));
    }

    @Test
    void databaseRejectsAnEmptyIdempotencyKey() {
        givenUserAndScholarship();
        assertViolates("ck_notification_events_key", () -> insertRaw(
                "", "SUBSCRIPTION_CONFIRMATION", null, "PENDING", 0, null, null));
    }

    @Test
    void eventForAnUnknownUserIsRejectedByTheForeignKey() {
        givenUserAndScholarship();
        assertViolates("fk_notification_events_user", () -> jdbc.update(
                "insert into notification_events (user_id, event_type, idempotency_key, scheduled_at) "
                        + "values (?, 'SUBSCRIPTION_CONFIRMATION', 'orphan', now())", UUID.randomUUID()));
    }

    @Test
    void deletingTheUserDeletesTheirEvents() {
        givenUserAndScholarship();
        events.saveAndFlush(new NotificationEvent(user, scholarship, NotificationEventType.DEADLINE_REMINDER, WHEN, "cascade-key"));
        em.clear();

        jdbc.update("delete from users where id = ?", user.getId());

        Integer left = jdbc.queryForObject("select count(*) from notification_events where user_id = ?", Integer.class, user.getId());
        assertThat(left).isZero();
    }

    @Test
    void aScholarshipReferencedByAnEventCannotBeDeleted() {
        givenUserAndScholarship();
        events.saveAndFlush(new NotificationEvent(user, scholarship, NotificationEventType.DEADLINE_REMINDER, WHEN, "restrict-key"));
        em.clear();

        assertViolates("fk_notification_events_scholarship",
                () -> jdbc.update("delete from scholarships where id = ?", scholarship.getId()));
    }
}
