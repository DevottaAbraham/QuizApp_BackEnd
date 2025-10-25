package Chruch_Of_God_Dindigul.Bible_quize.model;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Add the missing 'text' column that exists in your database
    @Column(name = "text", nullable = false)
    private String text;

    // English fields
    @Column(name = "text_en", nullable = false, length = 1024)
    private String text_en;

    @Column(name = "options_en", columnDefinition = "TEXT")
    private String options_en; // JSON array of strings

    // Add the missing 'correct_answer' column
    @Column(name = "correct_answer", nullable = false)
    private String correctAnswer;

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

    // The columnDefinition is a hint for Hibernate to correctly migrate existing string data to a timestamp.
    @Column(name = "release_date", columnDefinition = "TIMESTAMP")
    private LocalDateTime releaseDate;

    // The columnDefinition is a hint for Hibernate to correctly migrate existing string data to a timestamp.
    @Column(name = "disappear_date", columnDefinition = "TIMESTAMP")
    private LocalDateTime disappearDate;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;

    @UpdateTimestamp
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    // --- Manually added Getters and Setters to resolve compilation issues ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText_en() {
        return text_en;
    }

    public void setText_en(String text_en) {
        this.text_en = text_en;
    }

    public String getOptions_en() {
        return options_en;
    }

    public void setOptions_en(String options_en) {
        this.options_en = options_en;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getCorrectAnswer_en() {
        return correctAnswer_en;
    }

    public void setCorrectAnswer_en(String correctAnswer_en) {
        this.correctAnswer_en = correctAnswer_en;
    }

    public String getText_ta() {
        return text_ta;
    }

    public void setText_ta(String text_ta) {
        this.text_ta = text_ta;
    }

    public String getOptions_ta() {
        return options_ta;
    }

    public void setOptions_ta(String options_ta) {
        this.options_ta = options_ta;
    }

    public String getCorrectAnswer_ta() {
        return correctAnswer_ta;
    }

    public void setCorrectAnswer_ta(String correctAnswer_ta) {
        this.correctAnswer_ta = correctAnswer_ta;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDateTime releaseDate) {
        this.releaseDate = releaseDate;
    }

    public LocalDateTime getDisappearDate() {
        return disappearDate;
    }

    public void setDisappearDate(LocalDateTime disappearDate) {
        this.disappearDate = disappearDate;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}