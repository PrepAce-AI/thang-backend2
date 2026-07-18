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

    // Điểm
    private Double score;                 // /10
    private Integer totalQuestions;
    private Integer correctCount;
    private Double percentage;

    // ====== THÊM ======
    private Integer correctAnswers;       // alias của correctCount
    private Double accuracyPercent;       // alias của percentage
    private String level;
    private String levelColor;

    // AI
    private String summary;
    private String recommendedStartLevel;
    private List<String> recommendations;

    // Chi tiết
    private List<QuestionResultDetail> details;

    @Data
    @Builder
    public static class QuestionResultDetail {
        private Integer questionId;
        private String questionContent;
        private String selectedAnswer;
        private String correctAnswer;
        private Boolean isCorrect;
        private String explanation;
        private String topic;
    }
}