package ph.iskolarhub.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;

/** Asserts that PostgreSQL rejected a statement because of one specific, named constraint. */
public final class ConstraintAssertions {

    private ConstraintAssertions() {
    }

    public static void assertViolates(String constraintName, ThrowingCallable action) {
        Throwable thrown = catchThrowable(action);
        assertThat(thrown)
                .as("expected the database to reject the statement with constraint " + constraintName)
                .isInstanceOf(DataAccessException.class);
        String rootMessage = NestedExceptionUtils.getMostSpecificCause(thrown).getMessage();
        assertThat(rootMessage).contains(constraintName);
    }
}
