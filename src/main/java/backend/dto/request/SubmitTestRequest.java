package backend.dto.request;

import lombok.Data;

@Data
public class SubmitTestRequest {
    private Integer sessionsId;

    public SubmitTestRequest() {
    }

    public SubmitTestRequest(Integer sessionsId) {
        this.sessionsId = sessionsId;
    }

    public Integer getSessionsId() {
        return sessionsId;
    }

    public void setSessionsId(Integer sessionsId) {
        this.sessionsId = sessionsId;
    }
}
