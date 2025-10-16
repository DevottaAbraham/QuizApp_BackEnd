package Chruch_Of_God_Dindigul.Bible_quize.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDTO {
    private Long id;
    private String text_en;
    private List<String> options_en;
    private String correctAnswer_en;
    private String text_ta;
    private List<String> options_ta;
    private String correctAnswer_ta;
    private String status;
    private String releaseDate;
    private String disappearDate;
    private String authorUsername;
}