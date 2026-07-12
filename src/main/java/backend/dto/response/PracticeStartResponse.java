package backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Kết quả gọi API bắt đầu luyện đề: attemptId + 25 câu hỏi ngẫu nhiên. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeStartResponse {
    private Integer attemptId;
    private Integer quizId;
    private String quizTitle;
    private String subject;
    private String quizType;          // ENTRY_TEST | PRACTICE | MOCK_EXAM
    private Integer durationMinutes;
    /** Số giây còn lại — dùng cho resume khi F5 giữa chừng */
    private Integer remainingSeconds;
    private Integer totalQuestions;
    private List<PracticeQuestionDto> questions;
}
