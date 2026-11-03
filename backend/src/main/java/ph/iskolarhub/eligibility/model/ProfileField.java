package ph.iskolarhub.eligibility.model;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import ph.iskolarhub.scholarship.entity.RuleType;

/**
 * Student profile information an eligibility rule can ask for. GWA covers both the value and its scale, which
 * are always provided together. There is exactly one field per {@link RuleType}.
 */
public enum ProfileField {
    GWA("GWA"),
    FAMILY_INCOME("family income"),
    REGION("region"),
    COURSE("course"),
    YEAR_LEVEL("year level");

    private final String label;

    ProfileField(String label) {
        this.label = label;
    }

    /** Human-readable name used in fixed result messages. */
    public String label() {
        return label;
    }

    /** The profile field a rule of the given type reads. */
    public static ProfileField forRule(RuleType ruleType) {
        Objects.requireNonNull(ruleType, "ruleType");
        return switch (ruleType) {
            case GWA -> GWA;
            case FAMILY_INCOME -> FAMILY_INCOME;
            case REGION -> REGION;
            case COURSE -> COURSE;
            case YEAR_LEVEL -> YEAR_LEVEL;
        };
    }

    /** Unmodifiable, enum-ordered copy. Rejects null collections and null elements. */
    static Set<ProfileField> immutableSetOf(Collection<ProfileField> fields) {
        Objects.requireNonNull(fields, "fields");
        EnumSet<ProfileField> copy = EnumSet.noneOf(ProfileField.class);
        copy.addAll(fields);
        return Collections.unmodifiableSet(copy);
    }
}
