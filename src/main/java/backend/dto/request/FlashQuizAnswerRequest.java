package backend.dto.request;

import lombok.Data;

@Data
public class FlashQuizAnswerRequest {

    private Integer questionId;

    private String selectedAnswer;

}