package backend.dto.response;
import lombok.Data;
import java.util.*;

@Data
public class TestSessionResponse {
    private Integer sessionsId;
    private Integer quizId;
    private String quizTitle;
    private Integer remainingTime;
    private String status;
    private Date startedAt;

    public TestSessionResponse() {
    }

    public TestSessionResponse(Integer sessionsId, Integer quizId, String quizTitle, Integer remainingTime, String status, Date startedAt) {
        this.sessionsId = sessionsId;
        this.quizId = quizId;
        this.quizTitle = quizTitle;
        this.remainingTime = remainingTime;
        this.status = status;
        this.startedAt = startedAt;
    }

    public Integer getSessionsId() {
        return sessionsId;
    }

    public void setSessionsId(Integer sessionsId) {
        this.sessionsId = sessionsId;
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

    public Integer getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(Integer remainingTime) {
        this.remainingTime = remainingTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }
}
