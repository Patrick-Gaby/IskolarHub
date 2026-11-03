package ph.iskolarhub.eligibility.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import ph.iskolarhub.scholarship.entity.RuleType;

class RuleEvaluationResultTest {

    @ParameterizedTest
    @EnumSource(RuleType.class)
    void passedHasNoMissingFields(RuleType type) {
        RuleEvaluationResult result = RuleEvaluationResult.passed(type);

        assertThat(result.ruleType()).isEqualTo(type);
        assertThat(result.status()).isEqualTo(RuleEvaluationStatus.PASSED);
        assertThat(result.reason()).isEqualTo(RuleReason.REQUIREMENT_MET);
        assertThat(result.missingFields()).isEmpty();
        assertThat(result.message()).isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(RuleType.class)
    void failedHasNoMissingFields(RuleType type) {
        RuleEvaluationResult result = RuleEvaluationResult.failed(type);

        assertThat(result.status()).isEqualTo(RuleEvaluationStatus.FAILED);
        assertThat(result.reason()).isEqualTo(RuleReason.REQUIREMENT_NOT_MET);
        assertThat(result.missingFields()).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(RuleType.class)
    void missingProfileValueNamesExactlyTheFieldTheRuleReads(RuleType type) {
        RuleEvaluationResult result = RuleEvaluationResult.missingProfileValue(type);

        assertThat(result.status()).isEqualTo(RuleEvaluationStatus.INCOMPLETE);
        assertThat(result.reason()).isEqualTo(RuleReason.PROFILE_VALUE_MISSING);
        assertThat(result.missingFields()).containsExactly(ProfileField.forRule(type));
    }

    @Test
    void gwaScaleMismatchIsIncompleteAndAsksForGwa() {
        RuleEvaluationResult result = RuleEvaluationResult.gwaScaleMismatch();

        assertThat(result.ruleType()).isEqualTo(RuleType.GWA);
        assertThat(result.status()).isEqualTo(RuleEvaluationStatus.INCOMPLETE);
        assertThat(result.reason()).isEqualTo(RuleReason.GWA_SCALE_MISMATCH);
        assertThat(result.missingFields()).containsExactly(ProfileField.GWA);
    }

    @Test
    void everyRuleTypeMapsToADistinctProfileField() {
        Set<ProfileField> fields = Set.of(
                ProfileField.forRule(RuleType.GWA), ProfileField.forRule(RuleType.FAMILY_INCOME),
                ProfileField.forRule(RuleType.REGION), ProfileField.forRule(RuleType.COURSE),
                ProfileField.forRule(RuleType.YEAR_LEVEL));
        assertThat(fields).hasSize(RuleType.values().length);
    }

    @Test
    void messagesAreFixedTextWithoutValues() {
        assertThat(RuleEvaluationResult.passed(RuleType.GWA).message()).isEqualTo("Your profile meets the GWA requirement.");
        assertThat(RuleEvaluationResult.failed(RuleType.FAMILY_INCOME).message())
                .isEqualTo("Your profile does not meet the family income requirement.");
        assertThat(RuleEvaluationResult.missingProfileValue(RuleType.YEAR_LEVEL).message())
                .isEqualTo("Add your year level to your profile so this requirement can be checked.");
    }

    @Test
    void rejectsInvalidStatusReasonCombinations() {
        assertThatThrownBy(() -> new RuleEvaluationResult(RuleType.GWA, RuleEvaluationStatus.PASSED,
                RuleReason.REQUIREMENT_NOT_MET, "m", Set.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RuleEvaluationResult(RuleType.GWA, RuleEvaluationStatus.FAILED,
                RuleReason.REQUIREMENT_MET, "m", Set.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RuleEvaluationResult(RuleType.GWA, RuleEvaluationStatus.INCOMPLETE,
                RuleReason.REQUIREMENT_MET, "m", Set.of(ProfileField.GWA))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingFieldsOnPassedOrFailed() {
        assertThatThrownBy(() -> new RuleEvaluationResult(RuleType.GWA, RuleEvaluationStatus.PASSED,
                RuleReason.REQUIREMENT_MET, "m", Set.of(ProfileField.GWA))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RuleEvaluationResult(RuleType.GWA, RuleEvaluationStatus.FAILED,
                RuleReason.REQUIREMENT_NOT_MET, "m", Set.of(ProfileField.GWA))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void incompleteRequiresTheRulesOwnFieldOnly() {
        assertThatThrownBy(() -> new RuleEvaluationResult(RuleType.GWA, RuleEvaluationStatus.INCOMPLETE,
                RuleReason.PROFILE_VALUE_MISSING, "m", Set.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RuleEvaluationResult(RuleType.GWA, RuleEvaluationStatus.INCOMPLETE,
                RuleReason.PROFILE_VALUE_MISSING, "m", Set.of(ProfileField.REGION))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RuleEvaluationResult(RuleType.GWA, RuleEvaluationStatus.INCOMPLETE,
                RuleReason.PROFILE_VALUE_MISSING, "m", Set.of(ProfileField.GWA, ProfileField.REGION)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scaleMismatchIsOnlyValidForGwaRules() {
        assertThatThrownBy(() -> new RuleEvaluationResult(RuleType.REGION, RuleEvaluationStatus.INCOMPLETE,
                RuleReason.GWA_SCALE_MISMATCH, "m", Set.of(ProfileField.REGION))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullsAndBadMessages() {
        assertThatThrownBy(() -> new RuleEvaluationResult(null, RuleEvaluationStatus.PASSED, RuleReason.REQUIREMENT_MET,
                "m", Set.of())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RuleEvaluationResult(RuleType.GWA, RuleEvaluationStatus.PASSED,
                RuleReason.REQUIREMENT_MET, null, Set.of())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RuleEvaluationResult(RuleType.GWA, RuleEvaluationStatus.PASSED,
                RuleReason.REQUIREMENT_MET, "m", null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RuleEvaluationResult(RuleType.GWA, RuleEvaluationStatus.PASSED,
                RuleReason.REQUIREMENT_MET, "  ", Set.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RuleEvaluationResult(RuleType.GWA, RuleEvaluationStatus.PASSED,
                RuleReason.REQUIREMENT_MET, "x".repeat(301), Set.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missingFieldsAreDefensivelyCopiedAndUnmodifiable() {
        java.util.HashSet<ProfileField> source = new java.util.HashSet<>(Set.of(ProfileField.GWA));
        RuleEvaluationResult result = new RuleEvaluationResult(RuleType.GWA, RuleEvaluationStatus.INCOMPLETE,
                RuleReason.PROFILE_VALUE_MISSING, "m", source);

        source.add(ProfileField.REGION);

        assertThat(result.missingFields()).containsExactly(ProfileField.GWA);
        assertThatThrownBy(() -> result.missingFields().add(ProfileField.REGION))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
