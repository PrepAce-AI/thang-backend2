package backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StartQuizResponse {
    private Integer sessionsId;   // alias cho attemptId — frontend dùng sessionsId
    private Integer attemptId;
    private Integer quizId;
    private String quizTitle;
    private Integer durationMinutes;
    private Integer remainingTime; // giây = durationMinutes * 60
    private List<QuizResponse.QuestionResponse> questions;

    public StartQuizResponse(Integer sessionsId, Integer attemptId, Integer quizId,
                              String quizTitle, Integer durationMinutes, Integer remainingTime,
                              List<QuizResponse.QuestionResponse> questions) {
        this.sessionsId = sessionsId;
        this.attemptId = attemptId;
        this.quizId = quizId;
        this.quizTitle = quizTitle;
        this.durationMinutes = durationMinutes;
        this.remainingTime = remainingTime;
        this.questions = questions;
    }

    public Integer getSessionsId() { return sessionsId; }
    public void setSessionsId(Integer sessionsId) { this.sessionsId = sessionsId; }

    public Integer getAttemptId() { return attemptId; }
    public void setAttemptId(Integer attemptId) { this.attemptId = attemptId; }

    public Integer getQuizId() { return quizId; }
    public void setQuizId(Integer quizId) { this.quizId = quizId; }

    public String getQuizTitle() { return quizTitle; }
    public void setQuizTitle(String quizTitle) { this.quizTitle = quizTitle; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public Integer getRemainingTime() { return remainingTime; }
    public void setRemainingTime(Integer remainingTime) { this.remainingTime = remainingTime; }

    public List<QuizResponse.QuestionResponse> getQuestions() { return questions; }
    public void setQuestions(List<QuizResponse.QuestionResponse> questions) { this.questions = questions; }
}
