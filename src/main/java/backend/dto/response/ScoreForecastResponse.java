package backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** UC-29: Score Forecast — dự đoán điểm theo từng môn, tính từ dữ liệu bài làm thật. */
@Data
@Builder
public class ScoreForecastResponse {
    private boolean hasData;
    /** Điểm trung bình hiện tại (thang 10) */
    private Double currentTotal;
    /** Điểm dự đoán (thang 10) */
    private Double predictedTotal;
    /** Nhãn xu hướng: "Tăng +0.6 điểm" | "Giảm -0.3 điểm" | "Ổn định" */
    private String trend;
    /** Độ tin cậy dự đoán (0-100), tăng theo số lượt đã làm */
    private Integer confidence;
    private String summary;
    private List<SubjectForecast> subjects;

    @Data
    @Builder
    public static class SubjectForecast {
        private String subject;
        private Double currentAvg;
        private Double predictedScore;
        private String color;
    }
}
