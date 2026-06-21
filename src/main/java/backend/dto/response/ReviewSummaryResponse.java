package backend.dto.response;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ReviewSummaryResponse {
    private double averageRating;
    private int totalReviews;
    private Map<Integer, Long> ratingStarsCount; // Để React vẽ biểu đồ % sao
    private List<ReviewResponse> reviews;        // Tái sử dụng DTO ReviewResponse xịn của Toàn
}