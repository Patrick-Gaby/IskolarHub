package ph.iskolarhub.subscription.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static ph.iskolarhub.support.ConstraintAssertions.assertViolates;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import ph.iskolarhub.auth.entity.User;
import ph.iskolarhub.auth.repository.UserRepository;
import ph.iskolarhub.scholarship.entity.Scholarship;
import ph.iskolarhub.scholarship.repository.ScholarshipRepository;
import ph.iskolarhub.subscription.entity.Subscription;
import ph.iskolarhub.subscription.entity.SubscriptionStatus;
import ph.iskolarhub.support.PostgresDataJpaTest;
import ph.iskolarhub.support.TestData;

@PostgresDataJpaTest
class SubscriptionRepositoryTest {

    @Autowired
    SubscriptionRepository subscriptions;

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
        user = users.saveAndFlush(TestData.newUser("subscriber@example.com"));
        scholarship = scholarships.saveAndFlush(TestData.newScholarship("sub-target"));
    }

    @Test
    void subscriptionIsActiveWhenCreatedAndCanBeFoundByThePair() {
        givenUserAndScholarship();
        subscriptions.saveAndFlush(new Subscription(user, scholarship));
        em.clear();

        Subscription found = subscriptions.findByUserIdAndScholarshipId(user.getId(), scholarship.getId()).orElseThrow();

        assertThat(found.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(subscriptions.existsByUserIdAndScholarshipId(user.getId(), scholarship.getId())).isTrue();
        assertThat(subscriptions.existsByUserIdAndScholarshipId(user.getId(), UUID.randomUUID())).isFalse();
        assertThat(subscriptions.findByUserId(user.getId())).hasSize(1);
    }

    @Test
    void theSameUserAndScholarshipCannotBeSubscribedTwice() {
        givenUserAndScholarship();
        subscriptions.saveAndFlush(new Subscription(user, scholarship));
        assertViolates("uq_subscriptions_user_scholarship",
                () -> subscriptions.saveAndFlush(new Subscription(user, scholarship)));
    }

    @Test
    void unsubscribingKeepsTheRowAndResubscribingRestoresIt() {
        givenUserAndScholarship();
        Subscription sub = subscriptions.saveAndFlush(new Subscription(user, scholarship));

        sub.unsubscribe();
        subscriptions.saveAndFlush(sub);
        em.clear();
        assertThat(subscriptions.findById(sub.getId()).orElseThrow().getStatus()).isEqualTo(SubscriptionStatus.UNSUBSCRIBED);
        assertThat(subscriptions.findByUserId(user.getId())).hasSize(1);

        Subscription again = subscriptions.findById(sub.getId()).orElseThrow();
        again.resubscribe();
        subscriptions.saveAndFlush(again);
        em.clear();
        assertThat(subscriptions.findById(sub.getId()).orElseThrow().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    void deletingTheUserRemovesTheirSubscriptionsButNotTheScholarship() {
        givenUserAndScholarship();
        subscriptions.saveAndFlush(new Subscription(user, scholarship));
        em.clear();

        jdbc.update("delete from users where id = ?", user.getId());

        Integer left = jdbc.queryForObject("select count(*) from subscriptions where user_id = ?", Integer.class, user.getId());
        assertThat(left).isZero();
        assertThat(scholarships.existsById(scholarship.getId())).isTrue();
    }

    @Test
    void aScholarshipThatHasSubscriptionsCannotBeDeleted() {
        givenUserAndScholarship();
        subscriptions.saveAndFlush(new Subscription(user, scholarship));
        em.clear();

        assertViolates("fk_subscriptions_scholarship",
                () -> jdbc.update("delete from scholarships where id = ?", scholarship.getId()));
    }

    @Test
    void subscriptionForAnUnknownUserIsRejectedByTheForeignKey() {
        givenUserAndScholarship();
        assertViolates("fk_subscriptions_user", () -> jdbc.update(
                "insert into subscriptions (user_id, scholarship_id) values (?, ?)", UUID.randomUUID(), scholarship.getId()));
    }

    @Test
    void subscriptionForAnUnknownScholarshipIsRejectedByTheForeignKey() {
        givenUserAndScholarship();
        assertViolates("fk_subscriptions_scholarship", () -> jdbc.update(
                "insert into subscriptions (user_id, scholarship_id) values (?, ?)", user.getId(), UUID.randomUUID()));
    }

    @Test
    void databaseRejectsAnUnknownStatus() {
        givenUserAndScholarship();
        assertViolates("ck_subscriptions_status", () -> jdbc.update(
                "insert into subscriptions (user_id, scholarship_id, status) values (?, ?, 'PAUSED')",
                user.getId(), scholarship.getId()));
    }
}
