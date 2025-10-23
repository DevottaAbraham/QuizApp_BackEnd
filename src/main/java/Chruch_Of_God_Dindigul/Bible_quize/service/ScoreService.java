package Chruch_Of_God_Dindigul.Bible_quize.service;

import Chruch_Of_God_Dindigul.Bible_quize.dto.AnsweredQuestionDTO;
import Chruch_Of_God_Dindigul.Bible_quize.dto.MonthlyPerformanceDTO;
import Chruch_Of_God_Dindigul.Bible_quize.dto.LeaderboardDTO;
import Chruch_Of_God_Dindigul.Bible_quize.dto.QuizResultDTO;
import Chruch_Of_God_Dindigul.Bible_quize.model.Score;
import Chruch_Of_God_Dindigul.Bible_quize.model.User;
import Chruch_Of_God_Dindigul.Bible_quize.repository.ScoreRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.google.gson.Gson;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import com.itextpdf.io.font.constants.StandardFonts;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import com.google.gson.reflect.TypeToken;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ScoreService {

    private final ScoreRepository scoreRepository;
    private final Gson gson = new Gson();
    private final Type answeredQuestionListType;

    @Autowired
    public ScoreService(ScoreRepository scoreRepository) {
        this.scoreRepository = scoreRepository;
        this.answeredQuestionListType = new TypeToken<List<AnsweredQuestionDTO>>(){}.getType();
    }

    public List<LeaderboardDTO> getLeaderboard() {
        // Assuming findLeaderboard returns a list already sorted by total score descending
        // Limit to top 5
        return scoreRepository.findLeaderboard().stream().limit(5).collect(Collectors.toList());
    }

    public ByteArrayInputStream generateLeaderboardPpt(List<LeaderboardDTO> leaderboard) {
        try (XMLSlideShow ppt = new XMLSlideShow(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSLFSlide slide = ppt.createSlide();
            XSLFTextShape title = slide.createTextBox();
            title.setText("Leaderboard");

            StringBuilder content = new StringBuilder();
            int rank = 1;
            for (LeaderboardDTO entry : leaderboard) {
                content.append(rank++).append(". ").append(entry.getUsername()).append(" - ").append(entry.getTotalScore()).append("\n");
            }

            XSLFTextShape body = slide.createTextBox();
            body.setAnchor(new java.awt.Rectangle(50, 100, 600, 400));
            body.setText(content.toString());

            ppt.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PPT", e);
        }
    }

    public QuizResultDTO calculateAndSaveScore(QuizResultDTO submission, User user) {
        int correctAnswers = 0;
        int totalQuestions = submission.answeredQuestions() != null ? submission.answeredQuestions().size() : 0;
        // Only iterate if answeredQuestions is not null
        if (totalQuestions > 0) {
            for (AnsweredQuestionDTO answeredQuestion : submission.answeredQuestions()) {
                if (answeredQuestion.isCorrect()) {
                    correctAnswers++;
                }
            }
        }
        Score score = new Score();
        score.setUser(user);
        score.setScoreValue(correctAnswers);
        score.setTotalQuestions(totalQuestions);
        // Store the detailed answers as a JSON string in the database
        score.setAnsweredQuestionsJson(gson.toJson(submission.answeredQuestions()));
        score.setQuizDate(LocalDateTime.now());
        Score savedScore = scoreRepository.save(score);

        // Return a DTO with the final score and saved ID
        return new QuizResultDTO(
            savedScore.getId(),
            savedScore.getQuizDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            savedScore.getScoreValue(),
            savedScore.getTotalQuestions(),
            submission.answeredQuestions(),
            user.getId(),
            user.getUsername()
        );
    }

    public List<QuizResultDTO> getScoreHistoryForUser(User user) {
        return scoreRepository.findByUserOrderByQuizDateDesc(user).stream()
            .map(score -> {
                return new QuizResultDTO(
                score.getId(),
                score.getQuizDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                score.getScoreValue(),
                score.getTotalQuestions(),
                null, // Answered questions are not needed for the history list
                user.getId(),
                user.getUsername()
            ); // Semicolon to end the return statement
        }) // Closing brace for the lambda block
            .collect(Collectors.toList());
    }


    public List<Score> getScoresForUserSince(User user, LocalDateTime since) {
        return scoreRepository.findByUserAndQuizDateAfter(user, since);
    }

    @Transactional(readOnly = true)
    public List<QuizResultDTO> getAllScores() {
        return scoreRepository.findAllByOrderByQuizDateDesc().stream()
            .map(score -> {
                return new QuizResultDTO(
                score.getId(),
                score.getQuizDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                score.getScoreValue(),
                score.getTotalQuestions(),
                null, // Answered questions are not needed for the full list
                score.getUser().getId(), // Pass the user ID from the score object
                score.getUser().getUsername()
            )
            ; // Semicolon to end the return statement
        }).collect(Collectors.toList());
    }

    public List<MonthlyPerformanceDTO> getMonthlyPerformance() {
        List<Score> allScores = scoreRepository.findAll();
        
        return allScores.stream()
            .collect(Collectors.groupingBy(
                score -> score.getQuizDate().withDayOfMonth(1).toLocalDate(), // Group by year and month
                Collectors.averagingDouble(Score::getScoreValue)
            ))
            .entrySet().stream()
            .map(entry -> new MonthlyPerformanceDTO(
                entry.getKey().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                entry.getValue()
            ))
            .sorted(Comparator.comparing(MonthlyPerformanceDTO::month))
            .collect(Collectors.toList());
    }

    public List<MonthlyPerformanceDTO> getMonthlyPerformanceForUser(User user) {
        List<Score> userScores = scoreRepository.findByUserOrderByQuizDateDesc(user);

        return userScores.stream()
            .collect(Collectors.groupingBy(
                score -> score.getQuizDate().withDayOfMonth(1).toLocalDate(), // Group by year and month
                Collectors.averagingDouble(Score::getScoreValue)
            ))
            .entrySet().stream()
            .map(entry -> new MonthlyPerformanceDTO(
                entry.getKey().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                entry.getValue()
            ))
            .sorted(Comparator.comparing(MonthlyPerformanceDTO::month))
            .collect(Collectors.toList());
    }
    public ByteArrayInputStream generateQuizResultPdf(QuizResultDTO result, String language) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            boolean isTamil = "ta".equalsIgnoreCase(language);
            var font = isTamil
                ? PdfFontFactory.createFont("classpath:fonts/NotoSansTamil-Regular.ttf", com.itextpdf.io.font.PdfEncodings.IDENTITY_H)
                : PdfFontFactory.createFont(StandardFonts.HELVETICA);

            String titleText = isTamil ? "தேர்வு முடிவுகள்" : "Quiz Results";
            document.add(new Paragraph(titleText).setFont(font).setBold().setFontSize(20).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph((isTamil ? "தேதி: " : "Date: ") + result.quizDate()).setFont(font));
            document.add(new Paragraph((isTamil ? "மதிப்பெண்: " : "Score: ") + result.score()).setFont(font));
            document.add(new Paragraph("\n"));

            int questionNumber = 1;
            for (AnsweredQuestionDTO answeredQuestion : result.answeredQuestions()) {
                String questionText = isTamil ? answeredQuestion.questionText_ta() : answeredQuestion.questionText_en();
                document.add(new Paragraph(questionNumber++ + ". " + questionText).setFont(font).setBold());

                if (answeredQuestion.isCorrect()) {
                    String correctText = isTamil ? "உங்கள் பதில் சரி: " : "Your correct answer: ";
                    String userAnswerText = isTamil ? answeredQuestion.userAnswer_ta() : answeredQuestion.userAnswer();
                    document.add(new Paragraph(correctText + userAnswerText)
 .setFont(font).setFontColor(ColorConstants.GREEN));
                } else {
                    String wrongText = isTamil ? "உங்கள் தவறான பதில்: " : "Your incorrect answer: ";
                    String userAnswerText = isTamil ? answeredQuestion.userAnswer_ta() : answeredQuestion.userAnswer();
                    document.add(new Paragraph(wrongText + userAnswerText)
                        .setFont(font).setFontColor(ColorConstants.RED));

                    String correctText = isTamil ? "சரியான பதில்: " : "Correct answer: ";
                    String correctAnswerText = isTamil ? answeredQuestion.correctAnswer_ta() : answeredQuestion.correctAnswer();
                    document.add(new Paragraph(correctText + correctAnswerText)
                        .setFont(font).setFontColor(ColorConstants.GREEN));
                }
                document.add(new Paragraph("\n"));
            }

            document.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Quiz Result PDF", e);
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

    public ByteArrayInputStream generateQuizResultPdfById(Long scoreId, User user, String lang) {
        Score score = scoreRepository.findById(scoreId)
            .orElseThrow(() -> new RuntimeException("Score not found"));

        // Security check: ensure the user is requesting their own score
        if (!score.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You are not authorized to view this score.");
        }

        // Deserialize the stored JSON back into a list of answered questions
        List<AnsweredQuestionDTO> answeredQuestions = gson.fromJson(score.getAnsweredQuestionsJson(), answeredQuestionListType);

        QuizResultDTO resultDTO = new QuizResultDTO(score.getId(), score.getQuizDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), score.getScoreValue(), score.getTotalQuestions(), answeredQuestions, user.getId(), user.getUsername());

        // Now that we have the full result DTO, we can generate the PDF
        return generateQuizResultPdf(resultDTO, lang);
    }

    /**
     * Retrieves the detailed result of a specific quiz by its score ID.
     * Includes deserialized answered questions.
     * @param scoreId The ID of the score record.
     * @param user The authenticated user to ensure access control.
     * @return QuizResultDTO with full details.
     */
    public QuizResultDTO getScoreDetailById(Long scoreId, User user) {
        Score score = scoreRepository.findById(scoreId)
            .orElseThrow(() -> new RuntimeException("Score not found with ID: " + scoreId));

        // Security check: ensure the user is requesting their own score
        if (!score.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You are not authorized to view this score.");
        }

        List<AnsweredQuestionDTO> answeredQuestions = gson.fromJson(score.getAnsweredQuestionsJson(), answeredQuestionListType);
        return new QuizResultDTO(score.getId(), score.getQuizDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), score.getScoreValue(), score.getTotalQuestions(), answeredQuestions, user.getId(), user.getUsername());
    }
}
