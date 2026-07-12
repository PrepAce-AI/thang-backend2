package backend.dto.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class QuestionResponse {
    private Integer questionId;
    private String content;
    private List<OptionResponse> options;
    private String explanation; //Khi review moi return
    private Date createdAt;
    private Integer timestampSeconds;
    private String userFullName; // Trả tên người hỏi để hiển thị lên màn hình
    private String userAvatarUrl;
    private Integer userRoleId;
    private List<AnswerResponse> answers;

    // 1. THÊM THUỘC TÍNH NÀY ĐỂ TRẢ VỀ LOẠI CÂU HỎI Cho FRONTEND
    private String questionType;

    public QuestionResponse() {
    }

    // 2. CẬP NHẬT CONSTRUCTOR ĐẦY ĐỦ THAM SỐ
    public QuestionResponse(Integer questionId, String content, List<OptionResponse> options, String explanation, Date createdAt, Integer timestampSeconds, String userFullName, String userAvatarUrl, Integer userRoleId, List<AnswerResponse> answers, String questionType) {
        this.questionId = questionId;
        this.content = content;
        this.options = options;
        this.explanation = explanation;
        this.createdAt = createdAt;
        this.timestampSeconds = timestampSeconds;
        this.userFullName = userFullName;
        this.userAvatarUrl = userAvatarUrl;
        this.userRoleId = userRoleId;
        this.answers = answers;
        this.questionType = questionType; // Gán giá trị ở đây
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

    public List<OptionResponse> getOptions() {
        return options;
    }

    public void setOptions(List<OptionResponse> options) {
        this.options = options;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getTimestampSeconds() {
        return timestampSeconds;
    }

    public void setTimestampSeconds(Integer timestampSeconds) {
        this.timestampSeconds = timestampSeconds;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public void setUserAvatarUrl(String userAvatarUrl) {
        this.userAvatarUrl = userAvatarUrl;
    }

    public Integer getUserRoleId() {
        return userRoleId;
    }

    public void setUserRoleId(Integer userRoleId) {
        this.userRoleId = userRoleId;
    }

    public List<AnswerResponse> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AnswerResponse> answers) {
        this.answers = answers;
    }

    // 3. THÊM GETTER VÀ SETTER CHO QUESTION_TYPE Ở ĐÂY
    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }
}