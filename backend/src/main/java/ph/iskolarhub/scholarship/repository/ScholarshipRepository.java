package ph.iskolarhub.scholarship.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ph.iskolarhub.scholarship.entity.Scholarship;
import ph.iskolarhub.scholarship.entity.ScholarshipStatus;

public interface ScholarshipRepository extends JpaRepository<Scholarship, UUID> {

    Optional<Scholarship> findBySlug(String slug);

    /** Soonest deadline first; scholarships without a deadline come last. */
    List<Scholarship> findByStatusOrderByDeadlineAtAscNameAsc(ScholarshipStatus status);
}