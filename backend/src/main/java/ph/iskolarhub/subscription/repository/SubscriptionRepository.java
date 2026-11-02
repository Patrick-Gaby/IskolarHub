package ph.iskolarhub.subscription.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ph.iskolarhub.subscription.entity.Subscription;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByUserIdAndScholarshipId(UUID userId, UUID scholarshipId);

    boolean existsByUserIdAndScholarshipId(UUID userId, UUID scholarshipId);

    List<Subscription> findByUserId(UUID userId);

    // Deferred to Phase 8: "active subscribers of a scholarship" for the deadline reminder scan.
}