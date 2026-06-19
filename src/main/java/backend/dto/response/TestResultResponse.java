package backend.dto.response;

import lombok.Data;

import java.util.List;
import java.util.Date;

@Data
public class TestResultResponse {
    private Integer sessionsId;
    private Float score;
    private Integer totalQuestions;
    private Integer correctAnswers;
    private Integer timeSpent;
    private Date submittedAt;
    private List<QuestionResult> questions;

    public TestResultResponse() {
    }

    public TestResultResponse(Integer sessionsId, Float score, Integer totalQuestions, Integer correctAnswers, Integer timeSpent, Date submittedAt, List<QuestionResult> questions) {
        this.sessionsId = sessionsId;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.correctAnswers = correctAnswers;
        this.timeSpent = timeSpent;
        this.submittedAt = submittedAt;
        this.questions = questions;
    }

    public Integer getSessionsId() {
        return sessionsId;
    }

    public void setSessionsId(Integer sessionsId) {
        this.sessionsId = sessionsId;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
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

    public Integer getTimeSpent() {
        return timeSpent;
    }

    public void setTimeSpent(Integer timeSpent) {
        this.timeSpent = timeSpent;
    }

    public Date getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Date submittedAt) {
        this.submittedAt = submittedAt;
    }

    public List<QuestionResult> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionResult> questions) {
        this.questions = questions;
    }
}