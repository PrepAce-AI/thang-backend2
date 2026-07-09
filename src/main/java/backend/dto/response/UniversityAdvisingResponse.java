package backend.dto.response;

import lombok.*;
import java.util.*;

@Data
@Builder
public class UniversityAdvisingResponse {
    private boolean hasData;
    private String block;
    private Double predictedScore;
    private String summary;
    private List<UniversitySuggestion> suggestions;
}
