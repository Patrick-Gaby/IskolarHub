package ph.iskolarhub.eligibility.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import ph.iskolarhub.profile.entity.GwaScale;

class EvaluationProfileTest {

    @Test
    void emptyProfileKeepsEveryValueNullNeverZero() {
        EvaluationProfile profile = EvaluationProfile.empty();

        assertThat(profile.gwa()).isNull();
        assertThat(profile.gwaScale()).isNull();
        assertThat(profile.familyIncomeAnnualPhp()).isNull();
        assertThat(profile.regionCode()).isNull();
        assertThat(profile.courseCode()).isNull();
        assertThat(profile.yearLevel()).isNull();
    }

    @Test
    void doesNotAlterProvidedValues() {
        EvaluationProfile profile = new EvaluationProfile(new BigDecimal("1.50"), GwaScale.PH_1_TO_5,
                new BigDecimal("180000.00"), "ncr", "bs_computer_science", 3);

        assertThat(profile.gwa()).isEqualByComparingTo("1.50");
        assertThat(profile.gwa().scale()).isEqualTo(2);
        assertThat(profile.regionCode()).isEqualTo("ncr");
        assertThat(profile.courseCode()).isEqualTo("bs_computer_science");
        assertThat(profile.yearLevel()).isEqualTo(3);
    }

    @Test
    void toleratesGwaWithoutScaleSoRulesCanReportIncomplete() {
        EvaluationProfile profile = new EvaluationProfile(new BigDecimal("1.50"), null, null, null, null, null);

        assertThat(profile.gwa()).isNotNull();
        assertThat(profile.gwaScale()).isNull();
    }
}
