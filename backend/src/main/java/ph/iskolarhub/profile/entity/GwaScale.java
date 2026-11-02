package ph.iskolarhub.profile.entity;

import java.math.BigDecimal;

/**
 * Grading systems a GWA can be expressed in. Values on different scales must never be compared directly.
 * PH_1_TO_5: 1.00 is best and 5.00 is failing (lower is better). PERCENTAGE and GPA_4_0: higher is better.
 */
public enum GwaScale {
    PH_1_TO_5("1.00", "5.00", true),
    PERCENTAGE("0.00", "100.00", false),
    GPA_4_0("0.00", "4.00", false);

    private final BigDecimal min;
    private final BigDecimal max;
    private final boolean lowerIsBetter;

    GwaScale(String min, String max, boolean lowerIsBetter) {
        this.min = new BigDecimal(min);
        this.max = new BigDecimal(max);
        this.lowerIsBetter = lowerIsBetter;
    }

    public boolean isLowerBetter() {
        return lowerIsBetter;
    }

    /** True when the value lies within this scale's valid range (inclusive). */
    public boolean accepts(BigDecimal value) {
        return value != null && value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }
}