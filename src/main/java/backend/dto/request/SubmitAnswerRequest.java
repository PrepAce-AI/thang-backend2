package backend.dto.request;

//AUTO SAVE DAP AN KHI XONG 1 CAU HOI
public class SubmitAnswerRequest {
    private Integer questionId;
    private Integer selectedOptionId;

    // 1. THÊM THUỘC TÍNH NÀY CHO TỰ LUẬN
    private String essayAnswer;

    public SubmitAnswerRequest() {
    }

    // Constructor cũ giữ nguyên để không lỗi code chỗ khác
    public SubmitAnswerRequest(Integer questionId, Integer selectedOptionId) {
        this.questionId = questionId;
        this.selectedOptionId = selectedOptionId;
    }

    // 2. THÊM CONSTRUCTOR NÀY ĐỂ SAU NÀY TIỆN DÙNG (NẾU CẦN)
    public SubmitAnswerRequest(Integer questionId, Integer selectedOptionId, String essayAnswer) {
        this.questionId = questionId;
        this.selectedOptionId = selectedOptionId;
        this.essayAnswer = essayAnswer;
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

    // 3. THÊM GETTER VÀ SETTER CHO ESSAY_ANSWER Ở ĐÂY
    public String getEssayAnswer() {
        return essayAnswer;
    }

    public void setEssayAnswer(String essayAnswer) {
        this.essayAnswer = essayAnswer;
    }
}