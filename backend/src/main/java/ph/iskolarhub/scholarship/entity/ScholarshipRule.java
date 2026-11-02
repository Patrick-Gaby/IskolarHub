package ph.iskolarhub.scholarship.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ph.iskolarhub.common.persistence.BaseEntity;

/**
 * One configured requirement of a scholarship. The JSON config is validated by the eligibility engine in
 * Phase 3; the shape per rule type is documented in the demo seed script.
 */
@Entity
@Table(name = "scholarship_rules")
public class ScholarshipRule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scholarship_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private Scholarship scholarship;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, updatable = false, length = 20)
    private RuleType ruleType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> config;

    protected ScholarshipRule() {
        // for JPA
    }

    public ScholarshipRule(Scholarship scholarship, RuleType ruleType, Map<String, Object> config) {
        this.scholarship = Objects.requireNonNull(scholarship, "scholarship");
        this.ruleType = Objects.requireNonNull(ruleType, "ruleType");
        replaceConfig(config);
    }

    public Scholarship getScholarship() {
        return scholarship;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public Map<String, Object> getConfig() {
        return Collections.unmodifiableMap(config);
    }

    public void replaceConfig(Map<String, Object> newConfig) {
        if (newConfig == null || newConfig.isEmpty()) {
            throw new IllegalArgumentException("config must not be empty");
        }
        this.config = new LinkedHashMap<>(newConfig);
    }
}