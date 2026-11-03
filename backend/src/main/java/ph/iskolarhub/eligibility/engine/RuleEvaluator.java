package ph.iskolarhub.eligibility.engine;

import ph.iskolarhub.eligibility.exception.InvalidRuleConfigurationException;
import ph.iskolarhub.eligibility.model.EvaluationProfile;
import ph.iskolarhub.eligibility.model.RuleDefinition;
import ph.iskolarhub.eligibility.model.RuleEvaluationResult;
import ph.iskolarhub.scholarship.entity.RuleType;

/**
 * Evaluates one kind of scholarship requirement. Implementations (Clusters 3.3 to 3.7) are plain Java: no
 * Spring, JPA, repositories or I/O, and they are deterministic and stateless.
 *
 * <p>Contract:
 * <ul>
 *   <li>{@link #evaluate} is called only with a rule whose type equals {@link #supportedRuleType()}; any other
 *       type is an {@link IllegalArgumentException}.</li>
 *   <li>A malformed or unsupported configuration throws {@link InvalidRuleConfigurationException}. It is never
 *       converted into an INCOMPLETE result and never treated as a pass.</li>
 *   <li>A valid configuration with missing or incomparable student information returns an INCOMPLETE result and
 *       never treats null as zero.</li>
 *   <li>The profile and rule are never modified, and result messages never contain profile values.</li>
 * </ul>
 */
public interface RuleEvaluator {

    /** The single rule type this evaluator handles. */
    RuleType supportedRuleType();

    /**
     * @throws InvalidRuleConfigurationException if the rule's configuration is invalid
     * @throws IllegalArgumentException          if the rule type is not {@link #supportedRuleType()}
     */
    RuleEvaluationResult evaluate(EvaluationProfile profile, RuleDefinition rule);
}
