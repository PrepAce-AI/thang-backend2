package backend.dto.request;

import lombok.Data;

@Data
public class StartTestRequest {
    private Integer quizId;

    public StartTestRequest() {
    }

    public StartTestRequest(Integer quizId) {
        this.quizId = quizId;
    }

    public Integer getQuizId() {
        return quizId;
    }

    public void setQuizId(Integer quizId) {
        this.quizId = quizId;
    }
}
