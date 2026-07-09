package backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/** Kết quả chấm bài Luyện Đề trả về cho Frontend. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeResultResponse {
    private Integer attemptId;
    private Integer quizId;
    private String quizTitle;
    private String subject;
    private String quizType;

    /** Điểm thang 10, làm tròn 2 chữ số */
    private Double score;
    /** % câu đúng (0-100) — hiển thị vòng tròn tiến độ */
    private Double percentage;
    /** Phân loại năng lực: Giỏi / Khá / Trung bình / Yếu */
    private String level;
    private Integer correctCount;
    private Integer wrongCount;
    private Integer unansweredCount;
    private Integer totalQuestions;

    private Date startedAt;
    private Date submittedAt;

    /** Chi tiết từng câu: đáp án đã chọn, đáp án đúng, lời giải */
    private List<PracticeQuestionReview> details;
}
