package Chruch_Of_God_Dindigul.Bible_quize.repository;

import Chruch_Of_God_Dindigul.Bible_quize.model.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    // Finds all notices sorted by creation date
    List<Notice> findAllByOrderByCreatedAtDesc();

    // Finds all global notices OR notices specifically targeted to the given user ID
    @Query("SELECT n FROM Notice n LEFT JOIN n.targetUsers tu WHERE n.isGlobal = true OR tu.id = :userId ORDER BY n.createdAt DESC")
    List<Notice> findNoticesForUser(@Param("userId") Long userId);
}