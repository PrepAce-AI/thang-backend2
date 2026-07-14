package backend.dto.response;

import lombok.*;
import java.util.*;

@Data
@Builder
public class UniversityAdvisingResponse {
    private boolean hasData;
    private String block;
    /** Tổng điểm dự đoán khối (thang 30 = tổng 3 môn) */
    private Double predictedScore;
    private String summary;
    private List<UniversitySuggestionView> suggestions;
}
