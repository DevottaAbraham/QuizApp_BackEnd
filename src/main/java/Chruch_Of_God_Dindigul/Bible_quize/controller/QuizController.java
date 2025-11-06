package Chruch_Of_God_Dindigul.Bible_quize.controller;

import Chruch_Of_God_Dindigul.Bible_quize.dto.QuestionDTO;
import Chruch_Of_God_Dindigul.Bible_quize.dto.QuizResultDTO;
import Chruch_Of_God_Dindigul.Bible_quize.model.Score;
import Chruch_Of_God_Dindigul.Bible_quize.model.User;
import Chruch_Of_God_Dindigul.Bible_quize.service.QuestionService;
import Chruch_Of_God_Dindigul.Bible_quize.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/quizzes")
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
            // No quiz is active right now. Return an empty list to match the method signature.
            return ResponseEntity.ok(Collections.emptyList());
        }

        // Now, check if the user has already submitted a score for this quiz window.
        // CRITICAL FIX: The check must be for scores submitted *within* the current quiz's active window.
        // The previous logic incorrectly blocked users if they had any score after the release date,
        // preventing them from taking future quizzes.
        LocalDateTime quizReleaseDate = activeQuestions.get(0).getReleaseDate(); // Assuming all active questions have the same window
        LocalDateTime quizDisappearDate = activeQuestions.get(0).getDisappearDate();

        boolean hasScoreInWindow = scoreService.getScoresForUserSince(currentUser, quizReleaseDate)
            .stream()
            .anyMatch(score -> !score.getQuizDate().isAfter(quizDisappearDate)); // Check if any score date is within the window

        if (hasScoreInWindow) {
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