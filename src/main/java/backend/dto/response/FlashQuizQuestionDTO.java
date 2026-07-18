package backend.dto.response;

import lombok.Data;

@Data
public class FlashQuizQuestionDTO {

    private String question;

    private String optionA;

    private String optionB;

    private String optionC;

    private String optionD;

    private String correctAnswer;

    private String explanation;
}