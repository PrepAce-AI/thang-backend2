package backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "FlashQuizAnswers")
public class FlashQuizAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    private Integer answerId;

    @Column(name = "selected_answer")
    private String selectedAnswer;

    @Column(name = "is_correct")
    private Boolean isCorrect;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flash_quiz_id")
    private FlashQuiz flashQuiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private FlashQuizQuestion question;

    // ======== Getter Setter ========

    public Integer getAnswerId() {
        return answerId;
    }

    public void setAnswerId(Integer answerId) {
        this.answerId = answerId;
    }

    public String getSelectedAnswer() {
        return selectedAnswer;
    }

    public void setSelectedAnswer(String selectedAnswer) {
        this.selectedAnswer = selectedAnswer;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean correct) {
        isCorrect = correct;
    }

    public FlashQuiz getFlashQuiz() {
        return flashQuiz;
    }

    public void setFlashQuiz(FlashQuiz flashQuiz) {
        this.flashQuiz = flashQuiz;
    }

    public FlashQuizQuestion getQuestion() {
        return question;
    }

    public void setQuestion(FlashQuizQuestion question) {
        this.question = question;
    }
}
