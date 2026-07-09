package backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Map;

@Data
public class SubmitQuizRequest {

    /**
     * sessionsId = attemptId từ startQuiz response.
     * Dùng để lấy lại quizId từ QuizAttempt.
     */
    private Integer sessionsId;

    /** quizId — có thể gửi trực tiếp hoặc sẽ được lấy từ sessionsId */
    private Integer quizId;

    /**
     * Map<questionId, optionContent>
     * key   = questionId (Integer)
     * value = nội dung text của đáp án chọn (String)
     */
    @NotEmpty(message = "answers must not be empty")
    private Map<Integer, String> answers;
}
