package backend.dto.response;

public class QuestionResult {
    private Integer questionId;
    private String content;
    private String selectedAnswer;
    private String correctedAnswer;
    private String explanation;
    private boolean isCorrect;

    public QuestionResult() {
    }

    public QuestionResult(Integer questionId, String content, String selectedAnswer, String correctedAnswer, String explanation, boolean isCorrect) {
        this.questionId = questionId;
        this.content = content;
        this.selectedAnswer = selectedAnswer;
        this.correctedAnswer = correctedAnswer;
        this.explanation = explanation;
        this.isCorrect = isCorrect;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSelectedAnswer() {
        return selectedAnswer;
    }

    public void setSelectedAnswer(String selectedAnswer) {
        this.selectedAnswer = selectedAnswer;
    }

    public String getCorrectedAnswer() {
        return correctedAnswer;
    }

    public void setCorrectedAnswer(String correctedAnswer) {
        this.correctedAnswer = correctedAnswer;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }
}
