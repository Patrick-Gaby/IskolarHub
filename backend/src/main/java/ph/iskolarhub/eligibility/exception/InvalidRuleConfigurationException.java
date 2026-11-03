package ph.iskolarhub.eligibility.exception;

import java.util.Objects;
import ph.iskolarhub.scholarship.entity.RuleType;

/**
 * A scholarship rule's JSON configuration is malformed or unsupported, so no reliable decision can be made.
 * This is a data/administration error, not a student error, and it is never converted into an eligibility
 * result. The message must describe the problem without echoing configuration or student values. Detailed
 * violation reporting is added in Cluster 3.2; the application layer handles this exception in Cluster 3.8.
 */
public class InvalidRuleConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final RuleType ruleType;

    public InvalidRuleConfigurationException(RuleType ruleType, String message) {
        super(message);
        this.ruleType = Objects.requireNonNull(ruleType, "ruleType");
    }

    public InvalidRuleConfigurationException(RuleType ruleType, String message, Throwable cause) {
        super(message, cause);
        this.ruleType = Objects.requireNonNull(ruleType, "ruleType");
    }

    public RuleType getRuleType() {
        return ruleType;
    }
}
