package Chruch_Of_God_Dindigul.Bible_quize.dto;

import java.util.List;

public record QuizResultDTO(
    Long quizId,
    String quizDate,
    int score,
    int totalQuestions,
    List<AnsweredQuestionDTO> answeredQuestions,
    Long userId,
    String username
) {}