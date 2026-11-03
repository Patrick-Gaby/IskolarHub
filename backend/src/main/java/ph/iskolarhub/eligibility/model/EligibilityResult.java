package ph.iskolarhub.eligibility.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Overall eligibility of one student for one scholarship, with the individual rule results.
 *
 * <p>Decision table (all rules combine with AND):
 * <ul>
 *   <li>any rule FAILED: INELIGIBLE (a definitive failure beats missing information)</li>
 *   <li>else any rule INCOMPLETE: INCOMPLETE</li>
 *   <li>else (all PASSED, or no rules at all): ELIGIBLE</li>
 * </ul>
 * Rule results are ordered by {@code RuleType} declaration order, so the output is deterministic regardless of
 * the order the rules were supplied in. At most one result per rule type is allowed (the database enforces the
 * same). The canonical constructor re-derives the status and missing fields and rejects inconsistent input, so
 * an instance can never contradict its own rule results. Prefer {@link #from(Collection)}.
 *
 * @param status        overall outcome
 * @param ruleResults   immutable, ordered rule results
 * @param missingFields union of the missing fields of all INCOMPLETE rules
 */
public record EligibilityResult(
        EligibilityStatus status,
        List<RuleEvaluationResult> ruleResults,
        Set<ProfileField> missingFields) {

    public EligibilityResult {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(ruleResults, "ruleResults");
        Objects.requireNonNull(missingFields, "missingFields");
        List<RuleEvaluationResult> sorted = sortedCopy(ruleResults);
        Set<ProfileField> actualMissing = ProfileField.immutableSetOf(missingFields);
        if (status != decide(sorted)) {
            throw new IllegalArgumentException("status does not match the rule results");
        }
        if (!actualMissing.equals(missingOf(sorted))) {
            throw new IllegalArgumentException("missingFields do not match the rule results");
        }
        ruleResults = List.copyOf(sorted);
        missingFields = actualMissing;
    }

    /** Combines individual rule results with AND logic. An empty collection means "no rules": ELIGIBLE. */
    public static EligibilityResult from(Collection<RuleEvaluationResult> results) {
        Objects.requireNonNull(results, "results");
        List<RuleEvaluationResult> sorted = sortedCopy(results);
        return new EligibilityResult(decide(sorted), sorted, missingOf(sorted));
    }

    private static List<RuleEvaluationResult> sortedCopy(Collection<RuleEvaluationResult> results) {
        List<RuleEvaluationResult> sorted = new ArrayList<>(results.size());
        for (RuleEvaluationResult result : results) {
            sorted.add(Objects.requireNonNull(result, "ruleResults must not contain null"));
        }
        sorted.sort(Comparator.comparing(RuleEvaluationResult::ruleType));
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).ruleType() == sorted.get(i - 1).ruleType()) {
                throw new IllegalArgumentException("duplicate rule type in rule results");
            }
        }
        return sorted;
    }

    private static EligibilityStatus decide(List<RuleEvaluationResult> results) {
        boolean anyIncomplete = false;
        for (RuleEvaluationResult result : results) {
            if (result.status() == RuleEvaluationStatus.FAILED) {
                return EligibilityStatus.INELIGIBLE;
            }
            if (result.status() == RuleEvaluationStatus.INCOMPLETE) {
                anyIncomplete = true;
            }
        }
        return anyIncomplete ? EligibilityStatus.INCOMPLETE : EligibilityStatus.ELIGIBLE;
    }

    private static Set<ProfileField> missingOf(List<RuleEvaluationResult> results) {
        List<ProfileField> missing = new ArrayList<>();
        for (RuleEvaluationResult result : results) {
            missing.addAll(result.missingFields());
        }
        return ProfileField.immutableSetOf(missing);
    }
}
