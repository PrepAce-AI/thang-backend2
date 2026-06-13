package backend.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class QuestionResponse {
    private Integer questionId;
    private String content;
    private List<OptionResponse> options;
    private String explanation; //Khi review moi return

    public QuestionResponse() {
    }

    public QuestionResponse(Integer questionId, String content, List<OptionResponse> options, String explanation) {
        this.questionId = questionId;
        this.content = content;
        this.options = options;
        this.explanation = explanation;
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
}
