package ph.iskolarhub.eligibility.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import ph.iskolarhub.scholarship.entity.RuleType;

class EligibilityResultTest {

    @Test
    void noRulesMeansEligible() {
        EligibilityResult result = EligibilityResult.from(List.of());

        assertThat(result.status()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThat(result.ruleResults()).isEmpty();
        assertThat(result.missingFields()).isEmpty();
    }

    @Test
    void allPassedIsEligible() {
        EligibilityResult result = EligibilityResult.from(List.of(
                RuleEvaluationResult.passed(RuleType.GWA),
                RuleEvaluationResult.passed(RuleType.FAMILY_INCOME),
                RuleEvaluationResult.passed(RuleType.REGION)));

        assertThat(result.status()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThat(result.missingFields()).isEmpty();
    }

    @Test
    void oneFailureIsIneligible() {
        EligibilityResult result = EligibilityResult.from(List.of(
                RuleEvaluationResult.passed(RuleType.GWA),
                RuleEvaluationResult.failed(RuleType.COURSE)));

        assertThat(result.status()).isEqualTo(EligibilityStatus.INELIGIBLE);
    }

    @Test
    void incompleteWithoutFailureIsIncompleteAndListsMissingFields() {
        EligibilityResult result = EligibilityResult.from(List.of(
                RuleEvaluationResult.passed(RuleType.GWA),
                RuleEvaluationResult.missingProfileValue(RuleType.FAMILY_INCOME),
                RuleEvaluationResult.passed(RuleType.REGION)));

        assertThat(result.status()).isEqualTo(EligibilityStatus.INCOMPLETE);
        assertThat(result.missingFields()).containsExactly(ProfileField.FAMILY_INCOME);
    }

    @Test
    void failureTakesPrecedenceOverIncomplete() {
        EligibilityResult result = EligibilityResult.from(List.of(
                RuleEvaluationResult.passed(RuleType.GWA),
                RuleEvaluationResult.missingProfileValue(RuleType.FAMILY_INCOME),
                RuleEvaluationResult.failed(RuleType.REGION)));

        assertThat(result.status()).isEqualTo(EligibilityStatus.INELIGIBLE);
        // Missing information is still reported so the student knows what else would be checked.
        assertThat(result.missingFields()).containsExactly(ProfileField.FAMILY_INCOME);
    }

    @Test
    void multipleIncompleteRulesAreAllReportedInEnumOrder() {
        EligibilityResult result = EligibilityResult.from(List.of(
                RuleEvaluationResult.missingProfileValue(RuleType.YEAR_LEVEL),
                RuleEvaluationResult.gwaScaleMismatch(),
                RuleEvaluationResult.missingProfileValue(RuleType.REGION)));

        assertThat(result.status()).isEqualTo(EligibilityStatus.INCOMPLETE);
        assertThat(result.missingFields())
                .containsExactly(ProfileField.GWA, ProfileField.REGION, ProfileField.YEAR_LEVEL);
    }

    @Test
    void ruleResultsAreOrderedDeterministicallyRegardlessOfInputOrder() {
        List<RuleEvaluationResult> shuffled = List.of(
                RuleEvaluationResult.passed(RuleType.YEAR_LEVEL),
                RuleEvaluationResult.passed(RuleType.COURSE),
                RuleEvaluationResult.passed(RuleType.GWA),
                RuleEvaluationResult.passed(RuleType.REGION),
                RuleEvaluationResult.passed(RuleType.FAMILY_INCOME));
        List<RuleEvaluationResult> reversed = new ArrayList<>(shuffled);
        java.util.Collections.reverse(reversed);

        EligibilityResult a = EligibilityResult.from(shuffled);
        EligibilityResult b = EligibilityResult.from(reversed);

        assertThat(a.ruleResults()).extracting(RuleEvaluationResult::ruleType)
                .containsExactly(RuleType.GWA, RuleType.FAMILY_INCOME, RuleType.REGION, RuleType.COURSE,
                        RuleType.YEAR_LEVEL);
        assertThat(a).isEqualTo(b);
    }

    @Test
    void rejectsDuplicateRuleTypes() {
        assertThatThrownBy(() -> EligibilityResult.from(List.of(
                RuleEvaluationResult.passed(RuleType.GWA), RuleEvaluationResult.failed(RuleType.GWA))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullCollectionAndNullElements() {
        assertThatThrownBy(() -> EligibilityResult.from(null)).isInstanceOf(NullPointerException.class);
        List<RuleEvaluationResult> withNull = new ArrayList<>();
        withNull.add(null);
        assertThatThrownBy(() -> EligibilityResult.from(withNull)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void resultIsDefensivelyCopiedAndImmutable() {
        List<RuleEvaluationResult> source = new ArrayList<>(List.of(RuleEvaluationResult.passed(RuleType.GWA)));
        EligibilityResult result = EligibilityResult.from(source);

        source.add(RuleEvaluationResult.failed(RuleType.REGION));

        assertThat(result.ruleResults()).hasSize(1);
        assertThat(result.status()).isEqualTo(EligibilityStatus.ELIGIBLE);
        assertThatThrownBy(() -> result.ruleResults().add(RuleEvaluationResult.passed(RuleType.COURSE)))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.missingFields().add(ProfileField.GWA))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void canonicalConstructorRejectsStatusThatContradictsRuleResults() {
        List<RuleEvaluationResult> failing = List.of(RuleEvaluationResult.failed(RuleType.GWA));
        assertThatThrownBy(() -> new EligibilityResult(EligibilityStatus.ELIGIBLE, failing, Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EligibilityResult(EligibilityStatus.INCOMPLETE, failing, Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void canonicalConstructorRejectsMissingFieldsThatContradictRuleResults() {
        List<RuleEvaluationResult> incomplete = List.of(RuleEvaluationResult.missingProfileValue(RuleType.REGION));
        assertThatThrownBy(() -> new EligibilityResult(EligibilityStatus.INCOMPLETE, incomplete, Set.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EligibilityResult(EligibilityStatus.INCOMPLETE, incomplete,
                Set.of(ProfileField.COURSE))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void canonicalConstructorAcceptsConsistentValues() {
        List<RuleEvaluationResult> incomplete = List.of(RuleEvaluationResult.missingProfileValue(RuleType.REGION));
        EligibilityResult result = new EligibilityResult(EligibilityStatus.INCOMPLETE, incomplete,
                Set.of(ProfileField.REGION));

        assertThat(result).isEqualTo(EligibilityResult.from(incomplete));
    }
}
