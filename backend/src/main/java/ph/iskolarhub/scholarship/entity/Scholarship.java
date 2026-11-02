package ph.iskolarhub.scholarship.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Pattern;
import ph.iskolarhub.common.persistence.BaseEntity;
import ph.iskolarhub.common.time.DeadlinePolicy;
import ph.iskolarhub.common.validation.TextNormalizer;

/** A scholarship listing. Rules live in {@link ScholarshipRule}; this entity does not evaluate eligibility. */
@Entity
@Table(name = "scholarships")
public class Scholarship extends BaseEntity {

    private static final Pattern SLUG = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");
    private static final Pattern URL = Pattern.compile("^https?://\\S+$");

    @Column(name = "slug", nullable = false, length = 100)
    private String slug;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "provider", nullable = false, length = 200)
    private String provider;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "benefits", columnDefinition = "text")
    private String benefits;

    @Column(name = "info_url", length = 2048)
    private String infoUrl;

    /** Set only when the provider published a calendar date; the instant is then the end of that date in Asia/Manila. */
    @Column(name = "deadline_date")
    private LocalDate deadlineDate;

    @Column(name = "deadline_at", columnDefinition = "timestamptz")
    private Instant deadlineAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ScholarshipStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", nullable = false, length = 20)
    private ScholarshipDataSource dataSource;

    protected Scholarship() {
        // for JPA
    }

    /** New scholarships start as DRAFT. */
    public Scholarship(String slug, String name, String provider, ScholarshipDataSource dataSource) {
        if (slug == null || !SLUG.matcher(slug).matches() || slug.length() > 100) {
            throw new IllegalArgumentException("slug must be lower-case words separated by single hyphens");
        }
        this.slug = slug;
        this.name = TextNormalizer.requireText(name, "name", 200);
        this.provider = TextNormalizer.requireText(provider, "provider", 200);
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.status = ScholarshipStatus.DRAFT;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getProvider() {
        return provider;
    }

    public String getDescription() {
        return description;
    }

    public String getBenefits() {
        return benefits;
    }

    public String getInfoUrl() {
        return infoUrl;
    }

    public LocalDate getDeadlineDate() {
        return deadlineDate;
    }

    public Instant getDeadlineAt() {
        return deadlineAt;
    }

    public ScholarshipStatus getStatus() {
        return status;
    }

    public ScholarshipDataSource getDataSource() {
        return dataSource;
    }

    public void updateDetails(String name, String provider, String description, String benefits, String infoUrl) {
        this.name = TextNormalizer.requireText(name, "name", 200);
        this.provider = TextNormalizer.requireText(provider, "provider", 200);
        this.description = TextNormalizer.optionalText(description, "description", 20_000);
        this.benefits = TextNormalizer.optionalText(benefits, "benefits", 20_000);
        String url = TextNormalizer.optionalText(infoUrl, "infoUrl", 2048);
        if (url != null && !URL.matcher(url).matches()) {
            throw new IllegalArgumentException("infoUrl must start with http:// or https://");
        }
        this.infoUrl = url;
    }

    /** Deadline given as a calendar date: stored as the end of that date in Asia/Manila. */
    public void useDateOnlyDeadline(LocalDate date) {
        Objects.requireNonNull(date, "date");
        this.deadlineDate = date;
        this.deadlineAt = DeadlinePolicy.endOfDay(date);
    }

    /** Deadline given as an exact instant (no calendar date). */
    public void useExactDeadline(Instant at) {
        this.deadlineAt = Objects.requireNonNull(at, "at");
        this.deadlineDate = null;
    }

    public void clearDeadline() {
        this.deadlineAt = null;
        this.deadlineDate = null;
    }

    public void publish() {
        this.status = ScholarshipStatus.PUBLISHED;
    }

    public void archive() {
        this.status = ScholarshipStatus.ARCHIVED;
    }
}