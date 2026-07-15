package backend.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class SubmitFlashQuizRequest {

    private Integer flashQuizId;

    private List<FlashQuizAnswerRequest> answers;

}