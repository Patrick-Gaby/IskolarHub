package ph.iskolarhub.eligibility.model;

/** Outcome of evaluating a single rule. */
public enum RuleEvaluationStatus {
    PASSED,
    FAILED,
    /** The rule could not be decided because the student's information is missing or not comparable. */
    INCOMPLETE
}
