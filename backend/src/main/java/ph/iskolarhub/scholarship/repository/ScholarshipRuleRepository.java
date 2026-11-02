package ph.iskolarhub.scholarship.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ph.iskolarhub.scholarship.entity.ScholarshipRule;

public interface ScholarshipRuleRepository extends JpaRepository<ScholarshipRule, UUID> {

    List<ScholarshipRule> findByScholarshipId(UUID scholarshipId);

    /** Loads the rules of many scholarships in one query (used when evaluating a whole catalog). */
    List<ScholarshipRule> findByScholarshipIdIn(Collection<UUID> scholarshipIds);
}