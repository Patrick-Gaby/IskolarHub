package ph.iskolarhub.eligibility.model;

import java.util.Objects;
import java.util.Set;
import ph.iskolarhub.scholarship.entity.RuleType;

/**
 * Result of evaluating one rule. The canonical constructor enforces the valid combinations:
 * <ul>
 *   <li>PASSED: reason REQUIREMENT_MET, no missing fields.</li>
 *   <li>FAILED: reason REQUIREMENT_NOT_MET, no missing fields.</li>
 *   <li>INCOMPLETE: reason PROFILE_VALUE_MISSING or GWA_SCALE_MISMATCH (the latter only for GWA rules), and the
 *       missing fields are exactly the one field this rule type reads.</li>
 * </ul>
 * Use the factory methods: they produce fixed, safe messages that never contain profile values, thresholds or
 * configuration contents.
 *
 * @param ruleType      the rule that was evaluated
 * @param status        outcome
 * @param reason        machine-readable reason
 * @param message       safe, human-readable explanation
 * @param missingFields profile information the student still has to provide (empty unless INCOMPLETE)
 */
public record RuleEvaluationResult(
        RuleType ruleType,
        RuleEvaluationStatus status,
        RuleReason reason,
        String message,
        Set<ProfileField> missingFields) {

    private static final int MAX_MESSAGE_LENGTH = 300;

    public RuleEvaluationResult {
        Objects.requireNonNull(ruleType, "ruleType");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(message, "message");
        if (message.isBlank() || message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("message must be 1 to " + MAX_MESSAGE_LENGTH + " characters");
        }
        missingFields = ProfileField.immutableSetOf(missingFields);

        switch (status) {
            case PASSED -> requireShape(reason == RuleReason.REQUIREMENT_MET && missingFields.isEmpty(),
                    "PASSED requires reason REQUIREMENT_MET and no missing fields");
            case FAILED -> requireShape(reason == RuleReason.REQUIREMENT_NOT_MET && missingFields.isEmpty(),
                    "FAILED requires reason REQUIREMENT_NOT_MET and no missing fields");
            case INCOMPLETE -> {
                boolean reasonOk = reason == RuleReason.PROFILE_VALUE_MISSING
                        || (reason == RuleReason.GWA_SCALE_MISMATCH && ruleType == RuleType.GWA);
                requireShape(reasonOk, "INCOMPLETE requires reason PROFILE_VALUE_MISSING, or GWA_SCALE_MISMATCH for a GWA rule");
                requireShape(missingFields.equals(Set.of(ProfileField.forRule(ruleType))),
                        "INCOMPLETE requires the missing fields to be exactly the field this rule reads");
            }
        }
    }

    private static void requireShape(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public static RuleEvaluationResult passed(RuleType ruleType) {
        ProfileField field = ProfileField.forRule(ruleType);
        return new RuleEvaluationResult(ruleType, RuleEvaluationStatus.PASSED, RuleReason.REQUIREMENT_MET,
                "Your profile meets the " + field.label() + " requirement.", Set.of());
    }

    public static RuleEvaluationResult failed(RuleType ruleType) {
        ProfileField field = ProfileField.forRule(ruleType);
        return new RuleEvaluationResult(ruleType, RuleEvaluationStatus.FAILED, RuleReason.REQUIREMENT_NOT_MET,
                "Your profile does not meet the " + field.label() + " requirement.", Set.of());
    }

    /** The student has not provided the value this rule needs. */
    public static RuleEvaluationResult missingProfileValue(RuleType ruleType) {
        ProfileField field = ProfileField.forRule(ruleType);
        return new RuleEvaluationResult(ruleType, RuleEvaluationStatus.INCOMPLETE, RuleReason.PROFILE_VALUE_MISSING,
                "Add your " + field.label() + " to your profile so this requirement can be checked.", Set.of(field));
    }

    /** The student's GWA is on a different grading scale than the scholarship requires. */
    public static RuleEvaluationResult gwaScaleMismatch() {
        return new RuleEvaluationResult(RuleType.GWA, RuleEvaluationStatus.INCOMPLETE, RuleReason.GWA_SCALE_MISMATCH,
                "Your GWA is recorded on a different grading scale than this scholarship uses, so it cannot be "
                        + "compared. Provide your GWA on the required scale.",
                Set.of(ProfileField.GWA));
    }
}
