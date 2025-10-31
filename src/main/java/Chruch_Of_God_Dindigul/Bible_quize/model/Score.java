package Chruch_Of_God_Dindigul.Bible_quize.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scores")
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // This annotation maps the 'scoreValue' field to the 'score' column in the database.
    @Column(name = "score", nullable = false)
    private Integer scoreValue;

    @Column(nullable = false)
    private LocalDateTime quizDate;

    // This field was missing from the entity, causing data integrity errors on save. It must match the column name in your database schema.
    @Column(name = "answered_questions_json", columnDefinition = "TEXT")
    private String answeredQuestionsJson;

    // This field corresponds to the 'total' column in the database.
    @Column(name = "total", nullable = false)
    private Integer total;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getScoreValue() {
        return scoreValue;
    }

    public void setScoreValue(Integer scoreValue) {
        this.scoreValue = scoreValue;
    }

    public LocalDateTime getQuizDate() {
        return quizDate;
    }

    public void setQuizDate(LocalDateTime quizDate) {
        this.quizDate = quizDate;
    }

    public String getAnsweredQuestionsJson() {
        return answeredQuestionsJson;
    }

    public void setAnsweredQuestionsJson(String answeredQuestionsJson) {
        this.answeredQuestionsJson = answeredQuestionsJson;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }
}