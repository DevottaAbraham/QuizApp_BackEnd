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
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.google.gson.Gson;
import Chruch_Of_God_Dindigul.Bible_quize.dto.QuestionDTO;
import com.itextpdf.io.font.FontProgramFactory;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import com.itextpdf.io.font.constants.StandardFonts;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import com.itextpdf.io.font.FontProgram;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Type;
import java.awt.Color;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import com.google.gson.reflect.TypeToken;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
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
        // CRITICAL FIX: The previous implementation was flawed.
        // This new approach uses a dedicated, efficient query in the repository.
        // It correctly calculates total scores and prevents null values.
        return scoreRepository.findLeaderboard().stream()
                .limit(5) // Limit to top 5 as per user request
                .collect(Collectors.toList());
    }

    public ByteArrayInputStream generateLeaderboardPpt(List<LeaderboardDTO> leaderboard) {
        try (XMLSlideShow ppt = new XMLSlideShow(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // Set slide size to widescreen 16:9 (1280x720)
            java.awt.Dimension slideSize = new java.awt.Dimension(1280, 720);
            ppt.setPageSize(slideSize);
            XSLFSlide slide = ppt.createSlide(); // Create a slide

            // Add a title
            XSLFTextShape title = slide.createTextBox();
            title.setAnchor(new Rectangle(100, 50, 1080, 80));
            XSLFTextParagraph p1 = title.addNewTextParagraph();
            XSLFTextRun r1 = p1.addNewTextRun();
            r1.setText("Top 5 Winners");
            r1.setFontFamily("Arial");
            r1.setFontSize(44.0); // Increased font size
            r1.setFontColor(new Color(0, 82, 129)); // A nice blue color
            p1.setTextAlign(org.apache.poi.sl.usermodel.TextParagraph.TextAlign.CENTER);
            
            // Create a table
            XSLFTable table = slide.createTable(leaderboard.size() + 1, 3); // +1 for header row
            table.setAnchor(new Rectangle(140, 150, 1000, 400));

            // --- Create Header Row ---
            XSLFTableRow headerRow = table.getRows().get(0);
            String[] headers = {"Rank", "Username", "Total Score"};
            org.apache.poi.sl.usermodel.TextParagraph.TextAlign[] headerAligns = {
                org.apache.poi.sl.usermodel.TextParagraph.TextAlign.CENTER,
                org.apache.poi.sl.usermodel.TextParagraph.TextAlign.LEFT,
                org.apache.poi.sl.usermodel.TextParagraph.TextAlign.RIGHT
            };

            for (int i = 0; i < headers.length; i++) {
                XSLFTableCell th = headerRow.getCells().get(i);
                XSLFTextParagraph p = th.addNewTextParagraph();
                th.setVerticalAlignment(org.apache.poi.sl.usermodel.VerticalAlignment.MIDDLE);
                p.setTextAlign(headerAligns[i]);
                
                XSLFTextRun r = p.addNewTextRun();
                r.setText(headers[i]);
                r.setBold(true);
                r.setFontFamily("Arial");
                r.setFontSize(20.0); // Set header font size
                r.setFontColor(Color.white);
                th.setFillColor(new Color(0, 82, 129));
            }

            // --- Populate Data Rows ---
            int rank = 1;
            for (LeaderboardDTO entry : leaderboard) {
                XSLFTableRow row = table.getRows().get(rank);
                String[] values = {String.valueOf(rank), entry.getUsername(), String.valueOf(entry.getTotalScore())};
                org.apache.poi.sl.usermodel.TextParagraph.TextAlign[] cellAligns = {
                    org.apache.poi.sl.usermodel.TextParagraph.TextAlign.CENTER,
                    org.apache.poi.sl.usermodel.TextParagraph.TextAlign.LEFT,
                    org.apache.poi.sl.usermodel.TextParagraph.TextAlign.RIGHT
                };

                for (int i = 0; i < values.length; i++) {
                    XSLFTableCell cell = row.getCells().get(i);
                    cell.setVerticalAlignment(org.apache.poi.sl.usermodel.VerticalAlignment.MIDDLE);
                    XSLFTextParagraph p = cell.addNewTextParagraph();
                    p.setTextAlign(cellAligns[i]);
                    XSLFTextRun r = p.addNewTextRun();
                    r.setFontFamily("Arial");
                    r.setFontSize(18.0); // Increased font size
                    r.setText(values[i]);
                }
                rank++;
            }

            // Set column widths
            table.setColumnWidth(0, 100);
            table.setColumnWidth(1, 600);
            table.setColumnWidth(2, 300);

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
                // CRITICAL FIX: The constructor for QuizResultDTO requires all arguments.
                // Even if some are null, they must be passed to ensure correct field mapping.
                return new QuizResultDTO(
                    score.getId(), // quizId
                    score.getQuizDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), // quizDate
                    score.getScoreValue(), // score
                    score.getTotalQuestions(), // totalQuestions
                    null, // answeredQuestions (not needed for list view)
                    user.getId(), // userId
                    user.getUsername() // username
                );
            })
            .collect(Collectors.toList());
    }


    public List<Score> getScoresForUserSince(User user, LocalDateTime since) {
        return scoreRepository.findByUserAndQuizDateAfter(user, since);
    }

    @Transactional(readOnly = true)
    public List<QuizResultDTO> getAllScores() {
        return scoreRepository.findAllByOrderByQuizDateDesc().stream()
            .map(score -> new QuizResultDTO(
                score.getId(), // quizId
                score.getQuizDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), // quizDate
                score.getScoreValue(), // score
                score.getTotalQuestions(), // totalQuestions
                null, // answeredQuestions (not needed for list view)
                score.getUser().getId(), // userId
                score.getUser().getUsername() // username
            ))
            .collect(Collectors.toList());
    }

    @Cacheable("monthlyPerformance")
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

            // Enable the CalligraphHandler for complex script processing.
            // CalligraphHandler.enable(pdf); // Removed as requested.

            boolean isTamil = "ta".equalsIgnoreCase(language);
            // Explicitly declare the type to resolve compilation error
            PdfFont font;
            if (isTamil) {
                // CRITICAL FIX: Load the font from the classpath. This is the correct way to bundle
                // resources and ensures the font can be found when the app is packaged as a JAR.
                try (InputStream fontStream = getClass().getClassLoader().getResourceAsStream("fonts/NotoSansTamil-Regular.ttf")) {
                    if (fontStream == null) {
                        throw new IOException("Tamil font 'NotoSansTamil-Regular.ttf' not found in classpath under 'fonts/' directory.");
                    }
                    byte[] fontBytes = fontStream.readAllBytes();
                    FontProgram fontProgram = FontProgramFactory.createFont(fontBytes);
                    // FINAL FIX: Use the correct createFont method signature with an EmbeddingStrategy.
                    // The font-asian dependency will automatically handle complex script rendering (like Tamil)
                    // when the font is properly embedded with IDENTITY_H encoding.
                    font = PdfFontFactory.createFont(fontProgram, com.itextpdf.io.font.PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
                }
            } else {
                font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            }

            // Set the default font for the entire document. This is cleaner.
            document.setFont(font);

            String titleText = "Quiz Results";
            document.add(new Paragraph(titleText).setFont(font).setBold().setFontSize(22).setTextAlignment(TextAlignment.CENTER));
            
            // Add User Details and Score
            // Use Tamil labels for user details when the language is Tamil.
            document.add(new Paragraph("User: " + result.username() + " (ID: " + result.userId() + ")").setFont(font).setFontSize(12));
            document.add(new Paragraph((isTamil ? "தேதி: " : "Date: ") + result.quizDate()).setFont(font).setFontSize(12));
            document.add(new Paragraph((isTamil ? "மதிப்பெண்: " : "Score: ") + result.score() + "/" + result.totalQuestions()).setFont(font).setFontSize(12).setBold());
            document.add(new Paragraph("\n"));

            int questionNumber = 1;
            for (AnsweredQuestionDTO answeredQuestion : result.answeredQuestions()) {
                String questionText = isTamil ? answeredQuestion.questionText_ta() : answeredQuestion.questionText_en();
                document.add(new Paragraph(questionNumber++ + ". " + questionText).setFont(font).setBold().setFontSize(14));
                
                // FIX: Use Text objects to prevent character shuffling in complex scripts like Tamil.
                // This ensures the iText layout engine correctly processes each part of the string.
                // FINAL FIX: Explicitly set the font on each Text object to guarantee correct rendering.
                if (answeredQuestion.isCorrect()) {
                    String correctText = isTamil ? "உங்கள் பதில் சரி: " : "Your correct answer: ";
                    String userAnswerText = isTamil ? answeredQuestion.userAnswer_ta() : answeredQuestion.userAnswer();
                    Paragraph p = new Paragraph().add(new com.itextpdf.layout.element.Text(correctText).setFont(font))
                                                 .add(new com.itextpdf.layout.element.Text(userAnswerText).setFont(font));
                    document.add(p.setFontColor(ColorConstants.GREEN).setFontSize(12));
                } else {
                    String wrongText = isTamil ? "உங்கள் தவறான பதில்: " : "Your incorrect answer: ";
                    String userAnswerText = isTamil ? answeredQuestion.userAnswer_ta() : answeredQuestion.userAnswer();
                    Paragraph p1 = new Paragraph().add(new com.itextpdf.layout.element.Text(wrongText).setFont(font))
                                                  .add(new com.itextpdf.layout.element.Text(userAnswerText).setFont(font));
                    document.add(p1.setFontColor(ColorConstants.RED).setFontSize(12));

                    String correctText = isTamil ? "சரியான பதில்: " : "Correct answer: ";
                    String correctAnswerText = isTamil ? answeredQuestion.correctAnswer_ta() : answeredQuestion.correctAnswer();
                    Paragraph p2 = new Paragraph().add(new com.itextpdf.layout.element.Text(correctText).setFont(font))
                                                  .add(new com.itextpdf.layout.element.Text(correctAnswerText).setFont(font));
                    document.add(p2.setFontColor(ColorConstants.DARK_GRAY).setFontSize(12));
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
        List<AnsweredQuestionDTO> answeredQuestions = gson.fromJson(score.getAnsweredQuestionsJson(), this.answeredQuestionListType);

        QuizResultDTO resultDTO = new QuizResultDTO(
            score.getId(), // quizId
            score.getQuizDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), // quizDate
            score.getScoreValue(), // score
            score.getTotalQuestions(), // totalQuestions
            answeredQuestions, // answeredQuestions
            user.getId(), // userId
            user.getUsername() // username
        );

        // Now that we have the full result DTO, we can generate the PDF
        return generateQuizResultPdf(resultDTO, lang);
    }

    /**
	 * Generates a plain text file for a specific quiz result in Tamil.
	 * @param scoreId The ID of the score record.
	 * @param user The authenticated user.
	 * @return A ByteArrayInputStream containing the UTF-8 encoded text file.
	 */
    public ByteArrayInputStream generateQuizResultTxtById(Long scoreId, User user) {
        Score score = scoreRepository.findById(scoreId)
            .orElseThrow(() -> new RuntimeException("Score not found"));

        // Security check: ensure the user is requesting their own score
        if (!score.getUser().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You are not authorized to view this score.");
        }

        // Deserialize the stored JSON back into a list of answered questions
        List<AnsweredQuestionDTO> answeredQuestions = gson.fromJson(score.getAnsweredQuestionsJson(), this.answeredQuestionListType);

        StringBuilder sb = new StringBuilder();
		sb.append("தேதி: ").append(score.getQuizDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
		sb.append("மதிப்பெண்: ").append(score.getScoreValue()).append("/").append(score.getTotalQuestions()).append("\n\n");

		int questionNumber = 1;
		for (AnsweredQuestionDTO answeredQuestion : answeredQuestions) {
			sb.append(questionNumber++).append(". ").append(answeredQuestion.questionText_ta()).append("\n");

			if (answeredQuestion.isCorrect()) {
				sb.append("   உங்கள் பதில் சரி: ").append(answeredQuestion.userAnswer_ta()).append("\n");
			} else {
				sb.append("   உங்கள் தவறான பதில்: ").append(answeredQuestion.userAnswer_ta()).append("\n");
				sb.append("   சரியான பதில்: ").append(answeredQuestion.correctAnswer_ta()).append("\n");
			}
			sb.append("\n");
		}

		// Convert the final string to a byte array using UTF-8 encoding to support Tamil characters.
		byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
		return new ByteArrayInputStream(bytes);
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
        // CRITICAL FIX: The QuizResultDTO constructor requires all fields. The quizId was missing here.
        // This was causing the 'undefined' error on subsequent actions like downloading the PDF from the modal.
        return new QuizResultDTO(
            score.getId(), // quizId
            score.getQuizDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), // quizDate
            score.getScoreValue(), // score
            score.getTotalQuestions(), // totalQuestions
            answeredQuestions, // answeredQuestions
            user.getId(), // userId
            user.getUsername() // username
        );
    }

	public ByteArrayInputStream generateQuestionsPdf(List<QuestionDTO> questions) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			PdfWriter writer = new PdfWriter(out);
			PdfDocument pdf = new PdfDocument(writer);
			Document document = new Document(pdf);

			// Enable the CalligraphHandler for complex script processing.
			// CalligraphHandler.enable(pdf); // Removed as requested.
			// Load Tamil font
			PdfFont tamilFont;
			try (InputStream fontStream = getClass().getClassLoader().getResourceAsStream("fonts/NotoSansTamil-Regular.ttf")) {
				if (fontStream == null) {
					throw new IOException("Tamil font 'NotoSansTamil-Regular.ttf' not found in classpath under 'fonts/' directory.");
				}
				byte[] fontBytes = fontStream.readAllBytes();
				FontProgram fontProgram = FontProgramFactory.createFont(fontBytes);
				tamilFont = PdfFontFactory.createFont(fontProgram, com.itextpdf.io.font.PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
			}

			// Load English font
			PdfFont englishFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

			document.add(new Paragraph("All Questions and Answers")
					.setFont(englishFont).setBold().setFontSize(22).setTextAlignment(TextAlignment.CENTER));
			document.add(new Paragraph("\n"));

			int questionNumber = 1;
			for (QuestionDTO question : questions) {
				// Question Text (English)
				document.add(new Paragraph(questionNumber + ". " + question.getText_en())
						.setFont(englishFont).setBold().setFontSize(14));
				// Question Text (Tamil)
				document.add(new Paragraph(questionNumber + ". " + question.getText_ta())
						.setFont(tamilFont).setBold().setFontSize(14));

				// Options (English)
				if (question.getOptions_en() != null && !question.getOptions_en().isEmpty()) {
					document.add(new Paragraph("Options (English):").setFont(englishFont).setFontSize(12));
					char optionChar = 'A';
					for (String option : question.getOptions_en()) {
						document.add(new Paragraph("  " + (optionChar++) + ". " + option)
								.setFont(englishFont).setFontSize(12));
					}
				}

				// Options (Tamil)
				if (question.getOptions_ta() != null && !question.getOptions_ta().isEmpty()) {
					document.add(new Paragraph("விருப்பங்கள் (தமிழ்):").setFont(tamilFont).setFontSize(12));
					char optionChar = 'அ'; // Tamil equivalent for options
					for (String option : question.getOptions_ta()) {
						document.add(new Paragraph("  " + (optionChar++) + ". " + option)
								.setFont(tamilFont).setFontSize(12));
					}
				}

				// Correct Answer (English)
				document.add(new Paragraph("Correct Answer (English): " + question.getCorrectAnswer_en())
						.setFont(englishFont).setFontSize(12).setFontColor(ColorConstants.BLUE));

				// Correct Answer (Tamil)
				document.add(new Paragraph("சரியான பதில் (தமிழ்): " + question.getCorrectAnswer_ta())
						.setFont(tamilFont).setFontSize(12).setFontColor(ColorConstants.BLUE));

				document.add(new Paragraph("\n")); // Add a line break between questions
				questionNumber++;
			}

			document.close();
		} catch (IOException e) {
			throw new RuntimeException("Failed to generate Questions PDF", e);
		}
		return new ByteArrayInputStream(out.toByteArray());
	}

	/**
	 * Generates a plain text file containing all questions and answers in Tamil.
	 * @param questions The list of questions to include in the file.
	 * @return A ByteArrayInputStream containing the UTF-8 encoded text file.
	 */
	public ByteArrayInputStream generateQuestionsTxt(List<QuestionDTO> questions) {
		StringBuilder sb = new StringBuilder();
		sb.append("அனைத்து கேள்விகள் மற்றும் பதில்கள்\n");
		sb.append("==================================\n\n");

		int questionNumber = 1;
		for (QuestionDTO question : questions) {
			sb.append(questionNumber).append(". ").append(question.getText_ta()).append("\n");

			if (question.getOptions_ta() != null && !question.getOptions_ta().isEmpty()) {
				sb.append("   விருப்பங்கள்:\n");
				char optionChar = 'அ';
				for (String option : question.getOptions_ta()) {
					sb.append("      ").append(optionChar++).append(") ").append(option).append("\n");
				}
			}

			sb.append("   சரியான பதில்: ").append(question.getCorrectAnswer_ta()).append("\n\n");
			questionNumber++;
		}

		// Convert the final string to a byte array using UTF-8 encoding to support Tamil characters.
		byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
		return new ByteArrayInputStream(bytes);
	}
}
