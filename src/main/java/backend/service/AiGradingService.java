package backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AiGradingService {

    public static class GradingResult {
        public Float score;
        public String comment;

        public GradingResult(Float score, String comment) {
            this.score = score;
            this.comment = comment;
        }
    }

    /**
     * Chấm điểm câu hỏi tự luận bằng AI.
     * Trả về điểm (theo thang 10) và nhận xét.
     */
    public GradingResult gradeEssay(String questionContent, String studentAnswer) {
        log.info("AI Grading requested for question: {}", questionContent);
        log.info("Student answer: {}", studentAnswer);

        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            return new GradingResult(0f, "Học sinh không trả lời câu hỏi này.");
        }

        // TODO: Tích hợp API Gemini / OpenAI thật sự tại đây.
        // Tạm thời mock logic chấm điểm.
        Float score;
        String comment;
        int length = studentAnswer.trim().length();

        if (length < 10) {
            score = 2.0f;
            comment = "Câu trả lời quá ngắn, chưa đủ ý. Cần giải thích rõ ràng hơn.";
        } else if (length < 50) {
            score = 5.0f;
            comment = "Có cố gắng nhưng ý chính chưa thực sự đầy đủ và sâu sắc.";
        } else {
            score = 8.5f;
            comment = "Câu trả lời tương đối tốt, diễn đạt rõ ràng, đủ ý chính.";
        }

        return new GradingResult(score, comment);
    }
}
