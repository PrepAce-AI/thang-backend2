package backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuizResponse {
    private Integer quizId;
    private String quizTitle;
    private Integer durationMinutes;
    private String quizType;
    private List<QuestionResponse> questions;

    @Data
    @Builder
    public static class QuestionResponse {
        private Integer questionId;
        private String questionContent;
        private Integer cognitiveLevel;
        private List<OptionResponse> options;
    }

    @Data
    @Builder
    public static class OptionResponse {
        private Integer optionId;
        private String optionContent;
    }
}
