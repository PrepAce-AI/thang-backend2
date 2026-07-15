package backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * UC-28: AI Gap Diagnosis — phân tích lỗ hổng kiến thức theo TỪNG CHỦ ĐỀ (topic),
 * tính trực tiếp từ PracticeAnswers/Questions thật của học sinh (không phải dữ liệu giả lập).
 */
@Data
@Builder
public class GapDiagnosisResponse {
    private boolean hasData;
    /** % câu đúng trên tổng số câu đã làm (0-100), xuyên suốt mọi lượt Entry Test/Luyện đề/Thi thử */
    private Integer overallAccuracy;
    /** Nhận xét tổng quan — ưu tiên Gemini, fallback rule-based nếu AI lỗi/hết quota */
    private String summary;
    private List<GapView> gaps;

    @Data
    @Builder
    public static class GapView {
        /** Môn học — ví dụ "Toán", "Vật Lý" */
        private String subject;
        /** Tên chủ đề (topic) — ví dụ "Phương trình bậc nhất" */
        private String topic;
        /** Hổng nặng | Chưa vững | Cần tinh chỉnh */
        private String severity;
        private String color;
        /** % câu đúng của riêng chủ đề này (0-100) */
        private Integer accuracy;
        private String recommendation;
    }
}
