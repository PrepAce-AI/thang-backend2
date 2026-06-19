package backend.dto.response;

import lombok.Data;

@Data
public class OptionResponse {
    private Integer optionId;
    private String content;

    public OptionResponse() {
    }

    public OptionResponse(Integer optionId, String content) {
        this.optionId = optionId;
        this.content = content;
    }

    public Integer getOptionId() {
        return optionId;
    }

    public void setOptionId(Integer optionId) {
        this.optionId = optionId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
