package Chruch_Of_God_Dindigul.Bible_quize.dto;

public record AnsweredQuestionDTO(
    String questionText,
    String userAnswer,
    String correctAnswer,
    boolean isCorrect
) {}