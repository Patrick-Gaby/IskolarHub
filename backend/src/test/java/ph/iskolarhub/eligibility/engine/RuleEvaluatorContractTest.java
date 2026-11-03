package ph.iskolarhub.eligibility.engine;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ph.iskolarhub.eligibility.exception.InvalidRuleConfigurationException;
import ph.iskolarhub.eligibility.model.EligibilityResult;
import ph.iskolarhub.eligibility.model.EligibilityStatus;
import ph.iskolarhub.eligibility.model.EvaluationProfile;
import ph.iskolarhub.eligibility.model.RuleDefinition;
import ph.iskolarhub.eligibility.model.RuleEvaluationResult;
import ph.iskolarhub.scholarship.entity.RuleType;

/** Shows the evaluator interface composes with the result model using throwaway stub evaluators. */
class RuleEvaluatorContractTest {

    /** Stub: passes when the year level is present, INCOMPLETE otherwise. */
    private static final class StubYearEvaluator implements RuleEvaluator {
        @Override
        public RuleType supportedRuleType() {
            return RuleType.YEAR_LEVEL;
        }

        @Override
        public RuleEvaluationResult evaluate(EvaluationProfile profile, RuleDefinition rule) {
            if (rule.ruleType() != supportedRuleType()) {
                throw new IllegalArgumentException("unsupported rule type");
            }
            if (rule.config().isEmpty()) {
                throw new InvalidRuleConfigurationException(RuleType.YEAR_LEVEL, "config is empty");
            }
            return profile.yearLevel() == null
                    ? RuleEvaluationResult.missingProfileValue(RuleType.YEAR_LEVEL)
                    : RuleEvaluationResult.passed(RuleType.YEAR_LEVEL);
        }
    }

    @Test
    void evaluatorResultsCombineThroughEligibilityResult() {
        RuleEvaluator evaluator = new StubYearEvaluator();
        RuleDefinition rule = new RuleDefinition(RuleType.YEAR_LEVEL, Map.of("min_year", 1, "max_year", 2));

        List<RuleEvaluationResult> results = new ArrayList<>();
        results.add(evaluator.evaluate(EvaluationProfile.empty(), rule));

        assertThat(EligibilityResult.from(results).status()).isEqualTo(EligibilityStatus.INCOMPLETE);
    }

    @Test
    void invalidConfigurationIsAnExceptionNotAResult() {
        RuleEvaluator evaluator = new StubYearEvaluator();
        RuleDefinition empty = new RuleDefinition(RuleType.YEAR_LEVEL, Map.of());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> evaluator.evaluate(EvaluationProfile.empty(), empty))
                .isInstanceOf(InvalidRuleConfigurationException.class)
                .isInstanceOfSatisfying(InvalidRuleConfigurationException.class,
                        e -> assertThat(e.getRuleType()).isEqualTo(RuleType.YEAR_LEVEL));
    }

    @Test
    void unsupportedRuleTypeIsRejected() {
        RuleEvaluator evaluator = new StubYearEvaluator();
        RuleDefinition gwa = new RuleDefinition(RuleType.GWA, Map.of("value", 1));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> evaluator.evaluate(EvaluationProfile.empty(), gwa))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
