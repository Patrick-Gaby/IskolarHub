package ph.iskolarhub.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import ph.iskolarhub.auth.entity.User;
import ph.iskolarhub.common.persistence.BaseEntity;
import ph.iskolarhub.common.validation.TextNormalizer;

/**
 * Educational and financial profile (one per user). Every field except the owner is optional: null means
 * "not provided", never zero. Eligibility is NOT evaluated here.
 */
@Entity
@Table(name = "student_profiles")
public class StudentProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private User user;

    @Column(name = "gwa", precision = 5, scale = 2)
    private BigDecimal gwa;

    @Enumerated(EnumType.STRING)
    @Column(name = "gwa_scale", length = 20)
    private GwaScale gwaScale;

    /** Annual gross family income in Philippine pesos. */
    @Column(name = "family_income_annual_php", precision = 14, scale = 2)
    private BigDecimal familyIncomeAnnualPhp;

    @Column(name = "region_code", length = 20)
    private String regionCode;

    @Column(name = "course_code", length = 50)
    private String courseCode;

    @Column(name = "year_level")
    private Integer yearLevel;

    protected StudentProfile() {
        // for JPA
    }

    /** Creates an empty profile for the user; call {@link #update} to fill it in. */
    public StudentProfile(User user) {
        this.user = Objects.requireNonNull(user, "user");
    }

    public User getUser() {
        return user;
    }

    public BigDecimal getGwa() {
        return gwa;
    }

    public GwaScale getGwaScale() {
        return gwaScale;
    }

    public BigDecimal getFamilyIncomeAnnualPhp() {
        return familyIncomeAnnualPhp;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public Integer getYearLevel() {
        return yearLevel;
    }

    /** Replaces all profile values at once. Any argument may be null ("not provided"). */
    public void update(
            BigDecimal gwa,
            GwaScale gwaScale,
            BigDecimal familyIncomeAnnualPhp,
            String regionCode,
            String courseCode,
            Integer yearLevel) {
        if ((gwa == null) != (gwaScale == null)) {
            throw new IllegalArgumentException("gwa and gwaScale must be provided together");
        }
        if (gwa != null) {
            requireAtMostTwoDecimals(gwa, "gwa");
            if (!gwaScale.accepts(gwa)) {
                throw new IllegalArgumentException("gwa is outside the range of " + gwaScale);
            }
        }
        if (familyIncomeAnnualPhp != null) {
            requireAtMostTwoDecimals(familyIncomeAnnualPhp, "familyIncomeAnnualPhp");
            if (familyIncomeAnnualPhp.signum() < 0) {
                throw new IllegalArgumentException("familyIncomeAnnualPhp must not be negative");
            }
        }
        if (yearLevel != null && (yearLevel < 1 || yearLevel > 8)) {
            throw new IllegalArgumentException("yearLevel must be between 1 and 8");
        }
        this.gwa = gwa;
        this.gwaScale = gwaScale;
        this.familyIncomeAnnualPhp = familyIncomeAnnualPhp;
        this.regionCode = TextNormalizer.code(regionCode, "regionCode", 20);
        this.courseCode = TextNormalizer.code(courseCode, "courseCode", 50);
        this.yearLevel = yearLevel;
    }

    private static void requireAtMostTwoDecimals(BigDecimal value, String field) {
        if (value.setScale(2, RoundingMode.HALF_UP).compareTo(value) != 0) {
            throw new IllegalArgumentException(field + " must have at most two decimal places");
        }
    }
}