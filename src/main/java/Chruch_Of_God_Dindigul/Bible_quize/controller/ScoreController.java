package Chruch_Of_God_Dindigul.Bible_quize.controller;

import Chruch_Of_God_Dindigul.Bible_quize.dto.LeaderboardDTO;
import Chruch_Of_God_Dindigul.Bible_quize.dto.MonthlyPerformanceDTO;
import Chruch_Of_God_Dindigul.Bible_quize.dto.QuizResultDTO;
import Chruch_Of_God_Dindigul.Bible_quize.model.User;
import Chruch_Of_God_Dindigul.Bible_quize.service.ScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.util.List;

@RestController
@RequestMapping("/api/scores")
public class ScoreController {

    private final ScoreService scoreService;

    @Autowired
    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardDTO>> getLeaderboard() {
        List<LeaderboardDTO> leaderboard = scoreService.getLeaderboard();
        return ResponseEntity.ok(leaderboard);
    }

    @GetMapping("/leaderboard/ppt")
    public ResponseEntity<InputStreamResource> downloadLeaderboardPpt() {
        List<LeaderboardDTO> leaderboard = scoreService.getLeaderboard();
        ByteArrayInputStream bis = scoreService.generateLeaderboardPpt(leaderboard);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=leaderboard.pptx");

        return ResponseEntity.ok().headers(headers).contentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.presentationml.presentation")).body(new InputStreamResource(bis));
    }

    @GetMapping("/leaderboard/pdf")
    public ResponseEntity<InputStreamResource> downloadLeaderboardPdf() {
        List<LeaderboardDTO> leaderboard = scoreService.getLeaderboard();
        ByteArrayInputStream bis = scoreService.generateLeaderboardPdf(leaderboard);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=leaderboard.pdf");

        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(new InputStreamResource(bis));
    }


    @GetMapping("/history")
    public ResponseEntity<List<QuizResultDTO>> getScoreHistory(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        List<QuizResultDTO> history = scoreService.getScoreHistoryForUser(currentUser);
        return ResponseEntity.ok(history);
    }

    /**
     * Gets the monthly performance data for the currently authenticated user.
     * This is for the user's own performance chart.
     * @param authentication The authenticated user.
     * @return A list of monthly performance data points.
     */
    @GetMapping("/my-performance")
    public ResponseEntity<List<MonthlyPerformanceDTO>> getMyPerformance(Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        List<MonthlyPerformanceDTO> performanceData = scoreService.getMonthlyPerformanceForUser(currentUser);
        return ResponseEntity.ok(performanceData);
    }
    /**
     * Gets the detailed result of a specific quiz by its score ID.
     * This is used by the frontend to display past quiz results.
     * @param scoreId The ID of the score record.
     * @param authentication The authenticated user.
     * @return QuizResultDTO containing the score details and answered questions.
     */
    @GetMapping("/history/{scoreId}")
    public ResponseEntity<QuizResultDTO> getScoreDetail(@PathVariable Long scoreId, Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        QuizResultDTO scoreDetail = scoreService.getScoreDetailById(scoreId, currentUser);
        return ResponseEntity.ok(scoreDetail);
    }


    /**
     * Downloads a detailed, color-coded PDF report of a specific quiz result.
     * @param scoreId The ID of the score record.
     * @param lang The language for the PDF ('en' or 'ta').
     */
    @GetMapping("/history/{scoreId}/download")
    public ResponseEntity<InputStreamResource> downloadQuizResultPdf(
            @PathVariable("scoreId") Long scoreId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication authentication) {
        User currentUser = (User) authentication.getPrincipal();
        ByteArrayInputStream bis = scoreService.generateQuizResultPdfById(scoreId, currentUser, lang);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=quiz-result-" + scoreId + ".pdf");

        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_PDF).body(new InputStreamResource(bis));
    }
}