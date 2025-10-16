package Chruch_Of_God_Dindigul.Bible_quize.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // English fields
    @Column(name = "text_en", nullable = false, length = 1024)
    private String text_en;

    @Column(name = "options_en", columnDefinition = "TEXT")
    private String options_en; // JSON array of strings

    @Column(name = "correct_answer_en", nullable = false)
    private String correctAnswer_en; // The correct option text

    // Tamil fields
    @Column(name = "text_ta", nullable = false, length = 1024)
    private String text_ta;

    @Column(name = "options_ta", columnDefinition = "TEXT")
    private String options_ta; // JSON array of strings

    @Column(name = "correct_answer_ta", nullable = false)
    private String correctAnswer_ta; // The correct option text

    @Column(nullable = false)
    private String status = "draft"; // e.g., 'draft', 'published'

    @Column(name = "release_date")
    private String releaseDate;

    @Column(name = "disappear_date")
    private String disappearDate;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;
}