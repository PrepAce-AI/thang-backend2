package backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class SubmitQuizRequest {

    @NotNull(message = "quizId is required")
    private Integer quizId;

    /**
     * Map<questionId, selectedAnswer>
     * selectedAnswer là nội dung text của đáp án chọn
     */
    @NotEmpty(message = "answers must not be empty")
    private Map<Integer, Integer> answers;
}
