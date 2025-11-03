package Chruch_Of_God_Dindigul.Bible_quize.repository;

import Chruch_Of_God_Dindigul.Bible_quize.model.Role;
import Chruch_Of_God_Dindigul.Bible_quize.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    long countByRole(Role role);
    @Query("SELECT u FROM User u WHERE u.refreshToken IS NOT NULL")
    List<User> findActiveUsers();
}