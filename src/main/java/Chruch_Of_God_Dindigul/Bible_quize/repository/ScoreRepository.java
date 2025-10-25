package Chruch_Of_God_Dindigul.Bible_quize.repository;

import Chruch_Of_God_Dindigul.Bible_quize.dto.LeaderboardDTO;
import Chruch_Of_God_Dindigul.Bible_quize.model.Score;
import Chruch_Of_God_Dindigul.Bible_quize.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Long> {
    List<Score> findByUserOrderByQuizDateDesc(User user);
    List<Score> findByUserAndQuizDateAfter(User user, LocalDateTime since);
    List<Score> findAllByOrderByQuizDateDesc();

    @Query("SELECT new Chruch_Of_God_Dindigul.Bible_quize.dto.LeaderboardDTO(s.user.username, SUM(s.scoreValue)) " +
           "FROM Score s GROUP BY s.user.username ORDER BY SUM(s.scoreValue) DESC")
    List<LeaderboardDTO> findLeaderboard();
}