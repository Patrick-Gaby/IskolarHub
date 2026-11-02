package ph.iskolarhub.support;

import ph.iskolarhub.auth.entity.User;
import ph.iskolarhub.scholarship.entity.Scholarship;
import ph.iskolarhub.scholarship.entity.ScholarshipDataSource;

/** Small factories for test entities. The password hash is an obvious placeholder, not a credential. */
public final class TestData {

    public static final String FAKE_PASSWORD_HASH = "not-a-real-hash-for-tests-only";

    private TestData() {
    }

    public static User newUser(String email) {
        return new User(email, FAKE_PASSWORD_HASH);
    }

    public static Scholarship newScholarship(String slug) {
        return new Scholarship(slug, "Test scholarship " + slug, "Test provider", ScholarshipDataSource.DEMO);
    }
}
