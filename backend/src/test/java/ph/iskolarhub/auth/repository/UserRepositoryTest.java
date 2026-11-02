package ph.iskolarhub.auth.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static ph.iskolarhub.support.ConstraintAssertions.assertViolates;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import ph.iskolarhub.auth.entity.User;
import ph.iskolarhub.auth.entity.UserStatus;
import ph.iskolarhub.support.PostgresDataJpaTest;
import ph.iskolarhub.support.TestData;

@PostgresDataJpaTest
class UserRepositoryTest {

    @Autowired
    UserRepository users;

    @Autowired
    TestEntityManager em;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void savedUserCanBeFoundByNormalizedEmail() {
        users.saveAndFlush(TestData.newUser("  Student@Example.COM "));
        em.clear();

        User found = users.findByEmail("student@example.com").orElseThrow();

        assertThat(found.getId()).isNotNull();
        assertThat(found.getEmail()).isEqualTo("student@example.com");
        assertThat(found.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isAfterOrEqualTo(found.getCreatedAt());
        assertThat(users.existsByEmail("student@example.com")).isTrue();
        assertThat(users.existsByEmail("other@example.com")).isFalse();
    }

    @Test
    void passwordIsStoredOnlyAsTheGivenHash() {
        users.saveAndFlush(TestData.newUser("hash@example.com"));
        String stored = jdbc.queryForObject(
                "select password_hash from users where email = 'hash@example.com'", String.class);
        assertThat(stored).isEqualTo(TestData.FAKE_PASSWORD_HASH);
    }

    @Test
    void secondUserWithTheSameNormalizedEmailIsRejected() {
        users.saveAndFlush(TestData.newUser("same@example.com"));
        assertViolates("uq_users_email", () -> users.saveAndFlush(TestData.newUser("SAME@Example.com")));
    }

    @Test
    void databaseRejectsAnEmailThatIsNotNormalized() {
        assertViolates("ck_users_email_normalized", () -> jdbc.update(
                "insert into users (email, password_hash) values ('Mixed@Example.com', 'x')"));
    }

    @Test
    void databaseRejectsAnEmailWithSurroundingWhitespace() {
        assertViolates("ck_users_email_normalized", () -> jdbc.update(
                "insert into users (email, password_hash) values (' padded@example.com', 'x')"));
    }

    @Test
    void databaseRejectsAnUnknownStatus() {
        assertViolates("ck_users_status", () -> jdbc.update(
                "insert into users (email, password_hash, status) values ('status@example.com', 'x', 'BANNED')"));
    }

    @Test
    void databaseGeneratesAUuidWhenTheIdIsOmitted() {
        jdbc.update("insert into users (email, password_hash) values ('raw@example.com', 'x')");
        Object id = jdbc.queryForObject("select id from users where email = 'raw@example.com'", Object.class);
        assertThat(id).isNotNull();
    }

    @Test
    void disablingAUserIsPersisted() {
        User user = users.saveAndFlush(TestData.newUser("disable@example.com"));
        user.disable();
        users.saveAndFlush(user);
        em.clear();
        assertThat(users.findById(user.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.DISABLED);
    }
}
