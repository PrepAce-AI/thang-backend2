package backend.dto;

import lombok.Data;

@Data
public class InVideoQuestionDto {
    private Integer id;
    private Integer timestampSeconds;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctOption;
}
