package Chruch_Of_God_Dindigul.Bible_quize.repository;

import Chruch_Of_God_Dindigul.Bible_quize.model.Role;
import Chruch_Of_God_Dindigul.Bible_quize.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Use Optional for better null handling
    Optional<User> findByUsername(String username);
    long countByRole(Role role);
    Optional<User> findByRefreshToken(String refreshToken);
}
