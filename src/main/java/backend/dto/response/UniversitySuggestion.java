package backend.dto.response;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UniversitySuggestion {
    private String universityName;
    private String major;
    private String admissionScore;
    private String reason;

    // NEW 🔥
    private Double matchScore; // 0 → 1
}
