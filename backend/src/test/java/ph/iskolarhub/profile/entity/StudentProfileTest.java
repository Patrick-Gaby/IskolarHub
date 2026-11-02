package ph.iskolarhub.profile.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import ph.iskolarhub.support.TestData;

class StudentProfileTest {

    private StudentProfile profile() {
        return new StudentProfile(TestData.newUser("owner@example.com"));
    }

    @Test
    void newProfileHasNoValuesAndNothingIsTreatedAsZero() {
        StudentProfile p = profile();
        assertThat(p.getGwa()).isNull();
        assertThat(p.getGwaScale()).isNull();
        assertThat(p.getFamilyIncomeAnnualPhp()).isNull();
        assertThat(p.getRegionCode()).isNull();
        assertThat(p.getCourseCode()).isNull();
        assertThat(p.getYearLevel()).isNull();
    }

    @Test
    void updateNormalizesCodesAndKeepsValues() {
        StudentProfile p = profile();
        p.update(new BigDecimal("1.75"), GwaScale.PH_1_TO_5, new BigDecimal("250000.50"), " iv-a ", "bs computer science", 3);
        assertThat(p.getGwa()).isEqualByComparingTo("1.75");
        assertThat(p.getGwaScale()).isEqualTo(GwaScale.PH_1_TO_5);
        assertThat(p.getFamilyIncomeAnnualPhp()).isEqualByComparingTo("250000.50");
        assertThat(p.getRegionCode()).isEqualTo("IV_A");
        assertThat(p.getCourseCode()).isEqualTo("BS_COMPUTER_SCIENCE");
        assertThat(p.getYearLevel()).isEqualTo(3);
    }

    @Test
    void blankCodesBecomeNotProvided() {
        StudentProfile p = profile();
        p.update(null, null, null, "  ", "", null);
        assertThat(p.getRegionCode()).isNull();
        assertThat(p.getCourseCode()).isNull();
    }

    @Test
    void gwaWithoutScaleIsRejected() {
        assertThatThrownBy(() -> profile().update(new BigDecimal("1.5"), null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void scaleWithoutGwaIsRejected() {
        assertThatThrownBy(() -> profile().update(null, GwaScale.PERCENTAGE, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void gwaRangesAreInclusiveAtBothEndsOfEachScale() {
        profile().update(new BigDecimal("1.00"), GwaScale.PH_1_TO_5, null, null, null, null);
        profile().update(new BigDecimal("5.00"), GwaScale.PH_1_TO_5, null, null, null, null);
        profile().update(new BigDecimal("0.00"), GwaScale.PERCENTAGE, null, null, null, null);
        profile().update(new BigDecimal("100.00"), GwaScale.PERCENTAGE, null, null, null, null);
        profile().update(new BigDecimal("0.00"), GwaScale.GPA_4_0, null, null, null, null);
        profile().update(new BigDecimal("4.00"), GwaScale.GPA_4_0, null, null, null, null);
    }

    @Test
    void gwaJustOutsideEachScaleIsRejected() {
        assertThatThrownBy(() -> profile().update(new BigDecimal("0.99"), GwaScale.PH_1_TO_5, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> profile().update(new BigDecimal("5.01"), GwaScale.PH_1_TO_5, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> profile().update(new BigDecimal("100.01"), GwaScale.PERCENTAGE, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> profile().update(new BigDecimal("4.01"), GwaScale.GPA_4_0, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aGradeValidOnOneScaleCanBeInvalidOnAnother() {
        // 85 is a fine percentage but impossible on the 1.00 to 5.00 scale.
        profile().update(new BigDecimal("85.00"), GwaScale.PERCENTAGE, null, null, null, null);
        assertThatThrownBy(() -> profile().update(new BigDecimal("85.00"), GwaScale.PH_1_TO_5, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void moreThanTwoDecimalPlacesAreRejected() {
        assertThatThrownBy(() -> profile().update(new BigDecimal("1.755"), GwaScale.PH_1_TO_5, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> profile().update(null, null, new BigDecimal("100.001"), null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroIncomeIsAllowedButNegativeIsNot() {
        profile().update(null, null, BigDecimal.ZERO, null, null, null);
        assertThatThrownBy(() -> profile().update(null, null, new BigDecimal("-0.01"), null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void yearLevelBoundariesAreOneToEight() {
        profile().update(null, null, null, null, null, 1);
        profile().update(null, null, null, null, null, 8);
        assertThatThrownBy(() -> profile().update(null, null, null, null, null, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> profile().update(null, null, null, null, null, 9)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void malformedCodeIsRejected() {
        assertThatThrownBy(() -> profile().update(null, null, null, "NC$R", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
