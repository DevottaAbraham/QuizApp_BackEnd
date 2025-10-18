package Chruch_Of_God_Dindigul.Bible_quize.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoticeDTO {
    private Long id;
    private String title;
    private String content;
    private String imageUrl;
    private boolean isGlobal;
    private List<Long> targetUserIds;
    private LocalDateTime createdAt;
    private String authorUsername;
}