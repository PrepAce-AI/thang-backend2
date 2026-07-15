package backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Chi tiết chấm 1 câu trong màn hình kết quả. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeQuestionReview {
    private Integer questionId;
    private Integer questionOrder;
    private String questionContent;
    private String topic;
    private List<PracticeOptionReview> options;

    private Integer selectedOptionId;  // null = bỏ trống
    private Integer correctOptionId;
    private boolean correct;
    private boolean answered;

    /** Lời giải chi tiết — nguồn dữ liệu cho box "Giải thích lỗi sai ở đâu" */
    private String explanation;
    private String essayAnswer;
    private String questionType; // CHOICE, ESSAY, SHORT_ANSWER
    private Double score;
    private String teacherComment;
}
