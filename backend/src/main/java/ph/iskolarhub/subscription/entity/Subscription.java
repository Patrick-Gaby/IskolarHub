package ph.iskolarhub.subscription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import ph.iskolarhub.auth.entity.User;
import ph.iskolarhub.common.persistence.BaseEntity;
import ph.iskolarhub.scholarship.entity.Scholarship;

/** A user's notification subscription to a scholarship. One row per (user, scholarship); unsubscribing keeps the row. */
@Entity
@Table(name = "subscriptions")
public class Subscription extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scholarship_id", nullable = false, updatable = false, columnDefinition = "uuid")
    private Scholarship scholarship;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SubscriptionStatus status;

    protected Subscription() {
        // for JPA
    }

    public Subscription(User user, Scholarship scholarship) {
        this.user = Objects.requireNonNull(user, "user");
        this.scholarship = Objects.requireNonNull(scholarship, "scholarship");
        this.status = SubscriptionStatus.ACTIVE;
    }

    public User getUser() {
        return user;
    }

    public Scholarship getScholarship() {
        return scholarship;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void unsubscribe() {
        this.status = SubscriptionStatus.UNSUBSCRIBED;
    }

    public void resubscribe() {
        this.status = SubscriptionStatus.ACTIVE;
    }
}