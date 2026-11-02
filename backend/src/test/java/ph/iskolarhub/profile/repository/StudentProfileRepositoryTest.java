package ph.iskolarhub.profile.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static ph.iskolarhub.support.ConstraintAssertions.assertViolates;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import ph.iskolarhub.auth.entity.User;
import ph.iskolarhub.auth.repository.UserRepository;
import ph.iskolarhub.profile.entity.GwaScale;
import ph.iskolarhub.profile.entity.StudentProfile;
import ph.iskolarhub.support.PostgresDataJpaTest;
import ph.iskolarhub.support.TestData;

@PostgresDataJpaTest
class StudentProfileRepositoryTest {

    @Autowired
    StudentProfileRepository profiles;

    @Autowired
    UserRepository users;

    @Autowired
    TestEntityManager em;

    @Autowired
    JdbcTemplate jdbc;

    private User savedUser(String email) {
        return users.saveAndFlush(TestData.newUser(email));
    }

    @Test
    void profileWithAllValuesRoundTrips() {
        User user = savedUser("full@example.com");
        StudentProfile profile = new StudentProfile(user);
        profile.update(new BigDecimal("1.75"), GwaScale.PH_1_TO_5, new BigDecimal("250000.00"), " ncr ", "bs computer science", 2);
        profiles.saveAndFlush(profile);
        em.clear();

        StudentProfile loaded = profiles.findByUserId(user.getId()).orElseThrow();

        assertThat(loaded.getGwa()).isEqualByComparingTo("1.75");
        assertThat(loaded.getGwaScale()).isEqualTo(GwaScale.PH_1_TO_5);
        assertThat(loaded.getFamilyIncomeAnnualPhp()).isEqualByComparingTo("250000.00");
        assertThat(loaded.getRegionCode()).isEqualTo("NCR");
        assertThat(loaded.getCourseCode()).isEqualTo("BS_COMPUTER_SCIENCE");
        assertThat(loaded.getYearLevel()).isEqualTo(2);
        assertThat(loaded.getUser().getId()).isEqualTo(user.getId());
        assertThat(profiles.existsByUserId(user.getId())).isTrue();
    }

    @Test
    void missingValuesStayNullAndAreNeverStoredAsZero() {
        User user = savedUser("empty@example.com");
        profiles.saveAndFlush(new StudentProfile(user));
        em.clear();

        StudentProfile loaded = profiles.findByUserId(user.getId()).orElseThrow();

        assertThat(loaded.getGwa()).isNull();
        assertThat(loaded.getGwaScale()).isNull();
        assertThat(loaded.getFamilyIncomeAnnualPhp()).isNull();
        assertThat(loaded.getRegionCode()).isNull();
        assertThat(loaded.getCourseCode()).isNull();
        assertThat(loaded.getYearLevel()).isNull();
    }

    @Test
    void secondProfileForTheSameUserIsRejected() {
        User user = savedUser("twice@example.com");
        profiles.saveAndFlush(new StudentProfile(user));
        assertViolates("uq_student_profiles_user", () -> profiles.saveAndFlush(new StudentProfile(user)));
    }

    @Test
    void databaseRejectsGwaWithoutAScale() {
        User user = savedUser("pair@example.com");
        assertViolates("ck_student_profiles_gwa_pair", () -> jdbc.update(
                "insert into student_profiles (user_id, gwa) values (?, 1.50)", user.getId()));
    }

    @Test
    void databaseRejectsAScaleWithoutGwa() {
        User user = savedUser("pair2@example.com");
        assertViolates("ck_student_profiles_gwa_pair", () -> jdbc.update(
                "insert into student_profiles (user_id, gwa_scale) values (?, 'PERCENTAGE')", user.getId()));
    }

    @Test
    void databaseRejectsGwaOutsideTheRangeOfItsScale() {
        User user = savedUser("range@example.com");
        assertViolates("ck_student_profiles_gwa_range", () -> jdbc.update(
                "insert into student_profiles (user_id, gwa, gwa_scale) values (?, 5.50, 'PH_1_TO_5')", user.getId()));
    }

    @Test
    void databaseRejectsAPercentageOnTheOneToFiveScale() {
        User user = savedUser("scale@example.com");
        assertViolates("ck_student_profiles_gwa_range", () -> jdbc.update(
                "insert into student_profiles (user_id, gwa, gwa_scale) values (?, 85.00, 'PH_1_TO_5')", user.getId()));
    }

    @Test
    void databaseRejectsAnUnknownScale() {
        User user = savedUser("unknown@example.com");
        // An unknown scale breaks both the scale list and the range check. PostgreSQL reports whichever it
        // evaluates first, so the shared prefix of the two constraint names is asserted.
        assertViolates("ck_student_profiles_gwa_", () -> jdbc.update(
                "insert into student_profiles (user_id, gwa, gwa_scale) values (?, 1.50, 'LETTER')", user.getId()));
    }

    @Test
    void databaseRejectsNegativeIncome() {
        User user = savedUser("income@example.com");
        assertViolates("ck_student_profiles_income", () -> jdbc.update(
                "insert into student_profiles (user_id, family_income_annual_php) values (?, -1)", user.getId()));
    }

    @Test
    void databaseRejectsYearLevelOutsideOneToEight() {
        User user = savedUser("year@example.com");
        assertViolates("ck_student_profiles_year", () -> jdbc.update(
                "insert into student_profiles (user_id, year_level) values (?, 9)", user.getId()));
    }

    @Test
    void databaseRejectsLowerCaseRegionCode() {
        User user = savedUser("region@example.com");
        assertViolates("ck_student_profiles_region", () -> jdbc.update(
                "insert into student_profiles (user_id, region_code) values (?, 'ncr')", user.getId()));
    }

    @Test
    void profileForAnUnknownUserIsRejectedByTheForeignKey() {
        assertViolates("fk_student_profiles_user", () -> jdbc.update(
                "insert into student_profiles (user_id) values (?)", java.util.UUID.randomUUID()));
    }

    @Test
    void deletingTheUserDeletesTheirProfile() {
        User user = savedUser("cascade@example.com");
        profiles.saveAndFlush(new StudentProfile(user));
        em.clear();

        jdbc.update("delete from users where id = ?", user.getId());

        Integer left = jdbc.queryForObject(
                "select count(*) from student_profiles where user_id = ?", Integer.class, user.getId());
        assertThat(left).isZero();
    }
}
