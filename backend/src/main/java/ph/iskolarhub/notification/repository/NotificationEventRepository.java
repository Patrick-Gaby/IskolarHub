package ph.iskolarhub.notification.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ph.iskolarhub.notification.entity.NotificationEvent;

public interface NotificationEventRepository extends JpaRepository<NotificationEvent, UUID> {

    Optional<NotificationEvent> findByIdempotencyKey(String idempotencyKey);

    boolean existsByIdempotencyKey(String idempotencyKey);

    // Deferred to Phase 8: claiming due events (native FOR UPDATE SKIP LOCKED) and recovering stale claims.
}