package backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * UC-27: Adaptive Path — biểu đồ năng lực theo môn (radar chart) + lộ trình hành động,
 * tính từ dữ liệu bài làm thật của học sinh.
 */
@Data
@Builder
public class AdaptivePathViewResponse {
    private boolean hasData;
    private List<SkillView> skills;
    private List<PathStepView> path;
    /** Nhận xét tổng quan — ưu tiên Gemini, fallback rule-based nếu AI lỗi/hết quota */
    private String aiSummary;

    @Data
    @Builder
    public static class SkillView {
        private String subject;
        /** % câu đúng của môn này (0-100) — dùng làm % chiều dài thanh tiến độ / bán kính radar */
        private Integer score;
        private String color;
        private String status;
        private boolean warning;
    }

    @Data
    @Builder
    public static class PathStepView {
        /** review | practice | next — FE dùng để điều hướng khi bấm nút hành động */
        private String type;
        private String icon;
        private String title;
        private String subject;
        private String reason;
        private String action;
    }
}
