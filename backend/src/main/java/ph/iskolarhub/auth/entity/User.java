package ph.iskolarhub.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import ph.iskolarhub.common.persistence.BaseEntity;
import ph.iskolarhub.common.validation.TextNormalizer;

/** An IskolarHub account. Only the password HASH is ever stored. Never expose this entity through the API. */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    protected User() {
        // for JPA
    }

    public User(String email, String passwordHash) {
        this.email = TextNormalizer.email(email);
        this.passwordHash = TextNormalizer.requireText(passwordHash, "passwordHash", 255);
        this.status = UserStatus.ACTIVE;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void changePasswordHash(String newPasswordHash) {
        this.passwordHash = TextNormalizer.requireText(newPasswordHash, "passwordHash", 255);
    }

    public void disable() {
        this.status = UserStatus.DISABLED;
    }

    public void enable() {
        this.status = UserStatus.ACTIVE;
    }
}