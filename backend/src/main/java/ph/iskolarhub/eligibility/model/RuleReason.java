package ph.iskolarhub.eligibility.model;

/**
 * Machine-readable reason behind a {@link RuleEvaluationStatus}. Allowed combinations:
 * PASSED with REQUIREMENT_MET, FAILED with REQUIREMENT_NOT_MET, INCOMPLETE with PROFILE_VALUE_MISSING or
 * GWA_SCALE_MISMATCH.
 */
public enum RuleReason {
    REQUIREMENT_MET,
    REQUIREMENT_NOT_MET,
    /** The student has not provided the value this rule needs. Null is never treated as zero. */
    PROFILE_VALUE_MISSING,
    /** The student's GWA is on a different grading scale than the scholarship's. Scales are never converted. */
    GWA_SCALE_MISMATCH
}
