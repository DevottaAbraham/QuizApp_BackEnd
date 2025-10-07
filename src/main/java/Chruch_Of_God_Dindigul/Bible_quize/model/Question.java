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

    @Column(nullable = false, length = 1024)
    private String text;

    // In a real application, you might use a more structured type like a JSONB column
    // or a separate Options entity. For simplicity, we'll use a simple text field.
    @Column(columnDefinition = "TEXT")
    private String options; // e.g., "[\"Option A\", \"Option B\", \"Option C\"]"

    @Column(name = "correct_answer", nullable = false)
    private int correctAnswer; // Index of the correct answer

    @Column(nullable = false)
    private String status = "draft"; // e.g., 'draft', 'published'

    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;
}