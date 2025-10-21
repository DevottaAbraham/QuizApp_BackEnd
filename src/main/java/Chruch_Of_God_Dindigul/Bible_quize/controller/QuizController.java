package Chruch_Of_God_Dindigul.Bible_quize.controller;

import Chruch_Of_God_Dindigul.Bible_quize.dto.QuestionDTO;
import Chruch_Of_God_Dindigul.Bible_quize.dto.QuizResultDTO;
import Chruch_Of_God_Dindigul.Bible_quize.model.User;
import Chruch_Of_God_Dindigul.Bible_quize.service.QuestionService;
import Chruch_Of_God_Dindigul.Bible_quize.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
public class QuizController {

    private final QuestionService questionService;
    private final ScoreService scoreService;

    @Autowired
    public QuizController(QuestionService questionService, ScoreService scoreService) {
        this.questionService = questionService;
        this.scoreService = scoreService;
    }

    /**
     * Gets the list of all currently active (published) questions for the user to answer.
     */
    @GetMapping("/active")
    public ResponseEntity<List<QuestionDTO>> getActiveQuiz() {
 List<QuestionDTO> activeQuestions = questionService.getPublishedQuestionsForQuiz();
        return ResponseEntity.ok(activeQuestions);
    }

    /**
     * Receives the user's quiz submission, calculates the score, saves it,
     * and returns the detailed results.
     */
    @PostMapping("/submit")
    public ResponseEntity<QuizResultDTO> submitQuiz(@RequestBody QuizResultDTO submission, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        QuizResultDTO result = scoreService.calculateAndSaveScore(submission, currentUser);
        return ResponseEntity.ok(result);
    }
}