package ph.iskolarhub.auth.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import ph.iskolarhub.auth.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    /** The argument must already be normalized with TextNormalizer.email(...). */
    Optional<User> findByEmail(String normalizedEmail);

    boolean existsByEmail(String normalizedEmail);
}