package backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StartQuizResponse {
    private Integer attemptId;
    private Integer quizId;
    private String quizTitle;
    private List<QuizResponse.QuestionResponse> questions;

    public StartQuizResponse(Integer attemptId, Integer quizId, String quizTitle, List<QuizResponse.QuestionResponse> questions) {
        this.attemptId = attemptId;
        this.quizId = quizId;
        this.quizTitle = quizTitle;
        this.questions = questions;
    }

    public Integer getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Integer attemptId) {
        this.attemptId = attemptId;
    }

    public Integer getQuizId() {
        return quizId;
    }

    public void setQuizId(Integer quizId) {
        this.quizId = quizId;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public void setQuizTitle(String quizTitle) {
        this.quizTitle = quizTitle;
    }

    public List<QuizResponse.QuestionResponse> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuizResponse.QuestionResponse> questions) {
        this.questions = questions;
    }
}
