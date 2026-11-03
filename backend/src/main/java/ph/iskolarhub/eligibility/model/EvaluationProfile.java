package ph.iskolarhub.eligibility.model;

import java.math.BigDecimal;
import ph.iskolarhub.profile.entity.GwaScale;

/**
 * Immutable snapshot of the student information the engine reads. It is deliberately not the JPA entity: the
 * application service copies the values in. A null component means "not provided by the student" and is never
 * interpreted as zero. This record does not validate or normalize anything; profile rules are enforced when the
 * profile is saved, and a missing or inconsistent value simply makes the affected rule INCOMPLETE.
 *
 * @param gwa                   grade value on {@code gwaScale}
 * @param gwaScale              grading scale of {@code gwa}
 * @param familyIncomeAnnualPhp annual gross family income in Philippine pesos
 * @param regionCode            region code as stored on the profile
 * @param courseCode            course code as stored on the profile
 * @param yearLevel             current year level
 */
public record EvaluationProfile(
        BigDecimal gwa,
        GwaScale gwaScale,
        BigDecimal familyIncomeAnnualPhp,
        String regionCode,
        String courseCode,
        Integer yearLevel) {

    /** A profile in which the student has provided nothing. */
    public static EvaluationProfile empty() {
        return new EvaluationProfile(null, null, null, null, null, null);
    }
}
