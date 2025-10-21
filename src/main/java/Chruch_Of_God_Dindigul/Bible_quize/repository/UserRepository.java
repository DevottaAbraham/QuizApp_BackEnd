package Chruch_Of_God_Dindigul.Bible_quize.repository;

import Chruch_Of_God_Dindigul.Bible_quize.model.Role;
import Chruch_Of_God_Dindigul.Bible_quize.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Use Optional for better null handling
    Optional<User> findByUsername(String username);
    long countByRole(Role role);
    // This method will find all users who have a non-null refresh token,
    // indicating they have an active session.
    List<User> findByRefreshTokenIsNotNull();
}
