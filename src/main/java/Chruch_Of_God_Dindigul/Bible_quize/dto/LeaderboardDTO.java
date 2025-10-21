package Chruch_Of_God_Dindigul.Bible_quize.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardDTO {
    private String username;
    private Long totalScore;
}
