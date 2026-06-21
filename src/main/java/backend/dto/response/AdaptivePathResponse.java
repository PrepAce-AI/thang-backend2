package backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AdaptivePathResponse {

    private Integer studentId;
    private Double averageScore;
    private String overallLevel;         // Yếu / Trung bình / Khá / Giỏi

    /** Điểm mạnh: môn/chủ đề có % đúng cao */
    private List<String> strengths;

    /** Lỗ hổng kiến thức: chủ đề cần cải thiện */
    private List<GapItem> gaps;

    /** Lộ trình gợi ý: danh sách bài học ưu tiên */
    private List<LearningPathItem> recommendedPath;

    /** Biểu đồ năng lực: % đúng theo mức nhận thức */
    private Map<String, Double> cognitiveChart;

    @Data
    @Builder
    public static class GapItem {
        private String topic;
        private Double correctRate;
        private String recommendation;
    }

    @Data
    @Builder
    public static class LearningPathItem {
        private Integer lessonId;
        private String lessonTitle;
        private String priority;    // HIGH | MEDIUM | LOW
        private String reason;
    }
}
