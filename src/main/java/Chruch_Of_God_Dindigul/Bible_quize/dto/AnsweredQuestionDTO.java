package Chruch_Of_God_Dindigul.Bible_quize.dto;

import java.util.List;

public record AnsweredQuestionDTO(
    Long questionId,
    String questionText_en,
    String questionText_ta,
    String userAnswer,
    String userAnswer_ta,
    String correctAnswer,
    String correctAnswer_ta,
    boolean isCorrect,
    List<String> options_en,
    List<String> options_ta
) {}