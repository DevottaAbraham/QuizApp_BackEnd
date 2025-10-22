package Chruch_Of_God_Dindigul.Bible_quize.controller;

import Chruch_Of_God_Dindigul.Bible_quize.dto.QuestionDTO;
import Chruch_Of_God_Dindigul.Bible_quize.dto.QuizResultDTO;
import Chruch_Of_God_Dindigul.Bible_quize.model.Score;
import Chruch_Of_God_Dindigul.Bible_quize.model.User;
import Chruch_Of_God_Dindigul.Bible_quize.service.QuestionService;
import Chruch_Of_God_Dindigul.Bible_quize.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Collections;
import java.util.Map;

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

    @GetMapping("/active")
    public ResponseEntity<List<QuestionDTO>> getActiveQuiz(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        // Fetch all currently active questions first.
        List<QuestionDTO> activeQuestions = questionService.getPublishedQuestionsForQuiz();

        if (activeQuestions.isEmpty()) {
            // No quiz is active right now.
            return ResponseEntity.ok(Collections.emptyList());
        }

        // Now, check if the user has already submitted a score for this quiz window.
        List<Score> userScoresForThisWindow = scoreService.getScoresForUserSince(currentUser, activeQuestions.get(0).getReleaseDate());
        if (!userScoresForThisWindow.isEmpty()) {
            // User has already taken this quiz. Return an empty list.
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(activeQuestions);
    }

    @PostMapping("/submit")
    public ResponseEntity<QuizResultDTO> submitQuiz(@RequestBody QuizResultDTO submission, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        QuizResultDTO result = scoreService.calculateAndSaveScore(submission, currentUser);
        return ResponseEntity.ok(result);
    }
}