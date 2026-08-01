package backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "FlashQuizzes")
public class FlashQuiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flash_quiz_id")
    private Integer flashQuizId;

    @Column(name = "student_id")
    private Integer studentId;

    @Column(name = "chapter_id")
    private Integer chapterId;

    @Column(name = "total_questions")
    private Integer totalQuestions = 5;

    @Column(name = "correct_answers")
    private Integer correctAnswers = 0;

    @Column(name = "score")
    private BigDecimal score;

    @Column(name = "status")
    private String status = "GENERATED";

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt = new Date();

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "completed_at")
    private Date completedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private User student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "chapter_id", insertable = false, updatable = false)
    private Chapter chapter;

    @Column(name = "ai_model")
    private String aiModel;

    @OneToMany(mappedBy = "flashQuiz",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    private List<FlashQuizQuestion> questions;

    // ================= GETTER / SETTER =================

    public Integer getFlashQuizId() {
        return flashQuizId;
    }

    public void setFlashQuizId(Integer flashQuizId) {
        this.flashQuizId = flashQuizId;
    }

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public Integer getChapterId() {
        return chapterId;
    }

    public void setChapterId(Integer chapterId) {
        this.chapterId = chapterId;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public Integer getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(Integer correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public Chapter getChapter() {
        return chapter;
    }

    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    public List<FlashQuizQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<FlashQuizQuestion> questions) {
        this.questions = questions;
    }
}
