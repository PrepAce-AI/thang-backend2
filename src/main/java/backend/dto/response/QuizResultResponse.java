package backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuizResultResponse {
    private Integer attemptId;
    private Integer quizId;
    private String quizTitle;
    private Double score;           // Thang điểm 10
    private Integer totalQuestions;
    private Integer correctCount;
    private Double percentage;      // % đúng
    private String level;           // Phân loại: Yếu / Trung bình / Khá / Giỏi
    private List<QuestionResultDetail> details;

    @Data
    @Builder
    public static class QuestionResultDetail {
        private Integer questionId;
        private String questionContent;
        private String selectedAnswer;
        private String correctAnswer;
        private Boolean isCorrect;
        /** Lời giải chi tiết — hiển thị box "Giải thích chi tiết" trên trang kết quả */
        private String explanation;
        /** Chủ đề của câu hỏi — dùng cho AI phân tích lỗ hổng theo topic */
        private String topic;
    }
}
