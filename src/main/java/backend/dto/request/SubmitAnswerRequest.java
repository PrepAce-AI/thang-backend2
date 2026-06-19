package backend.dto.request;

//AUTO SAVE DAP AN KHI XONG 1 CAU HOI
public class SubmitAnswerRequest {
    private Integer questionId;
    private Integer selectedOptionId;

    public SubmitAnswerRequest() {
    }

    public SubmitAnswerRequest(Integer questionId, Integer selectedOptionId) {
        this.questionId = questionId;
        this.selectedOptionId = selectedOptionId;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public Integer getSelectedOptionId() {
        return selectedOptionId;
    }

    public void setSelectedOptionId(Integer selectedOptionId) {
        this.selectedOptionId = selectedOptionId;
    }
}
