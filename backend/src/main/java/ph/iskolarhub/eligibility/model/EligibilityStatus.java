package ph.iskolarhub.eligibility.model;

/** Overall outcome of evaluating one student profile against all rules of one scholarship. */
public enum EligibilityStatus {
    /** Every configured rule passed, or the scholarship has no configured rules. */
    ELIGIBLE,
    /** At least one configured rule definitively failed. A failure always wins over missing information. */
    INELIGIBLE,
    /** No rule failed, but at least one rule could not be evaluated because profile information is missing. */
    INCOMPLETE
}
