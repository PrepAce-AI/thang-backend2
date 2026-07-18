package backend.dto.response;

public class QuestionResult {
    private Integer questionId;
    private String content;
    private String selectedAnswer;
    private String correctedAnswer;
    private String explanation;
    private boolean isCorrect;

    // 3 Thuộc tính mới thêm
    private String questionType;
    private Float score;
    private String teacherComment;

    public QuestionResult() {
    }

    // Cập nhật Constructor đầy đủ tham số
    public QuestionResult(Integer questionId, String content, String selectedAnswer, String correctedAnswer, String explanation, boolean isCorrect, String questionType, Float score, String teacherComment) {
        this.questionId = questionId;
        this.content = content;
        this.selectedAnswer = selectedAnswer;
        this.correctedAnswer = correctedAnswer;
        this.explanation = explanation;
        this.isCorrect = isCorrect;
        this.questionType = questionType;
        this.score = score;
        this.teacherComment = teacherComment;
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

    // ─── BỔ SUNG CHÍNH XÁC CÁC GETTER VÀ SETTER CHO 3 BIẾN MỚI ───

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }

    public String getTeacherComment() {
        return teacherComment;
    }

    public void setTeacherComment(String teacherComment) {
        this.teacherComment = teacherComment;
    }
}