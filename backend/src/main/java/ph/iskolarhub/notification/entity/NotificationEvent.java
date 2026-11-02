package ph.iskolarhub.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import ph.iskolarhub.auth.entity.User;
import ph.iskolarhub.common.persistence.BaseEntity;
import ph.iskolarhub.common.validation.TextNormalizer;
import ph.iskolarhub.scholarship.entity.Scholarship;

/**
 * A durable notification request. Phase 2 only creates PENDING events; the claim/send/retry transitions are
 * implemented with the processor in Phase 8, which is why there are no state-changing methods yet.
 */
@Entity
@Table(name = "notification_events")
public class NotificationEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scholarship_id", updatable = false, columnDefinition = "uuid")
    private Scholarship scholarship;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 40)
    private NotificationEventType eventType;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 200)
    private String idempotencyKey;

    @Column(name = "scheduled_at", nullable = false, columnDefinition = "timestamptz")
    private Instant scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationEventStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_attempt_at", columnDefinition = "timestamptz")
    private Instant lastAttemptAt;

    @Column(name = "locked_at", columnDefinition = "timestamptz")
    private Instant lockedAt;

    @Column(name = "sent_at", columnDefinition = "timestamptz")
    private Instant sentAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    protected NotificationEvent() {
        // for JPA
    }

    /** @param scholarship may be null for events not tied to a scholarship (reminders require one). */
    public NotificationEvent(
            User user,
            Scholarship scholarship,
            NotificationEventType eventType,
            Instant scheduledAt,
            String idempotencyKey) {
        this.user = Objects.requireNonNull(user, "user");
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        if (eventType == NotificationEventType.DEADLINE_REMINDER && scholarship == null) {
            throw new IllegalArgumentException("a deadline reminder requires a scholarship");
        }
        this.scholarship = scholarship;
        this.scheduledAt = Objects.requireNonNull(scheduledAt, "scheduledAt");
        this.idempotencyKey = TextNormalizer.requireText(idempotencyKey, "idempotencyKey", 200);
        this.status = NotificationEventStatus.PENDING;
        this.attemptCount = 0;
    }

    public User getUser() {
        return user;
    }

    public Scholarship getScholarship() {
        return scholarship;
    }

    public NotificationEventType getEventType() {
        return eventType;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getScheduledAt() {
        return scheduledAt;
    }

    public NotificationEventStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public String getLastError() {
        return lastError;
    }
}