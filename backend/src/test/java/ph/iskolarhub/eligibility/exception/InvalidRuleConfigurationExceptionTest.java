package ph.iskolarhub.eligibility.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import ph.iskolarhub.scholarship.entity.RuleType;

class InvalidRuleConfigurationExceptionTest {

    @Test
    void carriesRuleTypeMessageAndCause() {
        RuntimeException cause = new RuntimeException("root");
        InvalidRuleConfigurationException e =
                new InvalidRuleConfigurationException(RuleType.FAMILY_INCOME, "max_amount is missing", cause);

        assertThat(e.getRuleType()).isEqualTo(RuleType.FAMILY_INCOME);
        assertThat(e.getMessage()).isEqualTo("max_amount is missing");
        assertThat(e.getCause()).isSameAs(cause);
    }

    @Test
    void isUncheckedAndRequiresRuleType() {
        assertThat(new InvalidRuleConfigurationException(RuleType.GWA, "m")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> new InvalidRuleConfigurationException(null, "m"))
                .isInstanceOf(NullPointerException.class);
    }
}
