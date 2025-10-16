package Chruch_Of_God_Dindigul.Bible_quize.repository;

import Chruch_Of_God_Dindigul.Bible_quize.model.Question;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    
    List<Question> findByStatus(String status);
    long countByStatus(String status);
}