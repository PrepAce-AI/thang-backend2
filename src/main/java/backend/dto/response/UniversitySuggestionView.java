package backend.dto.response;

import lombok.Builder;
import lombok.Data;

/** Gợi ý 1 trường/ngành trong UC-30 University Advising — field khớp với UniversityAdvisingPage.jsx */
@Data
@Builder
public class UniversitySuggestionView {
    private String university;
    private String major;
    /** % khả năng đỗ (0-100), tính từ chênh lệch điểm dự đoán so với điểm chuẩn tham khảo */
    private Integer chancePercent;
    /** Điểm chuẩn tham khảo, hiển thị dạng chuỗi (ví dụ "24.5") */
    private String benchmark;
    private String chanceLabel;
    private String color;
    private String note;
}
