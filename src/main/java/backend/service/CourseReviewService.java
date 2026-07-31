package backend.service;

import backend.dto.request.ReviewRequest;
import backend.dto.response.ReviewResponse;
import backend.dto.response.ReviewSummaryResponse;
import backend.entity.Course;
import backend.entity.CourseReview;
import backend.entity.User;
import backend.repository.CourseReviewRepository;
import backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseReviewService {

    @Autowired
    private CourseReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private backend.repository.CourseRepository courseRepository;

    @Transactional
    public ReviewResponse createReview(String email, Integer courseId, ReviewRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        CourseReview review = new CourseReview();
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setUser(user);
        review.setStudentId(user.getId());

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));
        review.setCourse(course);

        CourseReview saved = reviewRepository.save(review);

        // 🔥 SỬA CHỖ NÀY: Map thủ công an toàn, bốc trực tiếp dữ liệu từ object 'user' xịn phía trên
        ReviewResponse res = new ReviewResponse();
        res.setId(saved.getId());
        res.setRating(saved.getRating());
        res.setComment(saved.getComment());
        res.setCreatedAt(saved.getCreatedAt());

        // Lấy thẳng từ đối tượng 'user' đã tìm thấy bằng email đăng nhập
        res.setUserFullName(user.getFullName());
        res.setUserAvatarUrl(user.getAvatarUrl());

        return res;
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByCourse(Integer courseId) {
        return reviewRepository.findByCourse_CourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ReviewResponse mapToResponse(CourseReview review) {
        ReviewResponse res = new ReviewResponse();
        res.setId(review.getId());
        res.setRating(review.getRating());
        res.setComment(review.getComment());
        res.setCreatedAt(review.getCreatedAt());
        res.setUserFullName(review.getUser().getFullName());
        res.setUserAvatarUrl(review.getUser().getAvatarUrl());
        return res;
    }
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ReviewSummaryResponse getReviewSummary(Integer courseId) {
        // Lấy danh sách review gốc từ Repository của bạn
        List<CourseReview> reviewList = reviewRepository.findByCourse_CourseIdOrderByCreatedAtDesc(courseId);

        ReviewSummaryResponse summary = new ReviewSummaryResponse();
        summary.setTotalReviews(reviewList.size());

        // Nếu chưa có ai đánh giá, trả về dữ liệu trống an toàn để tránh React bị chia cho số 0
        if (reviewList.isEmpty()) {
            summary.setAverageRating(0.0);
            summary.setRatingStarsCount(java.util.Map.of(1, 0L, 2, 0L, 3, 0L, 4, 0L, 5, 0L));
            summary.setReviews(java.util.List.of());
            return summary;
        }

        // 1. Tính điểm trung bình (Ví dụ: rớt ra 4.6666 -> làm tròn thành 4.7)
        double avg = reviewList.stream().mapToInt(CourseReview::getRating).average().orElse(0.0);
        summary.setAverageRating(Math.round(avg * 10.0) / 10.0);

        // 2. Thống kê số lượng từng mức sao từ 1 đến 5 (Gom nhóm dữ liệu SQL)
        java.util.Map<Integer, Long> starsCount = new java.util.HashMap<>(java.util.Map.of(1, 0L, 2, 0L, 3, 0L, 4, 0L, 5, 0L));
        java.util.Map<Integer, Long> actualCounts = reviewList.stream()
                .collect(Collectors.groupingBy(CourseReview::getRating, Collectors.counting()));
        starsCount.putAll(actualCounts);
        summary.setRatingStarsCount(starsCount);

        // 3. Tái sử dụng hàm mapToResponse xịn của Toàn để chuyển sang List DTO
        List<ReviewResponse> details = reviewList.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        summary.setReviews(details);

        return summary;
    }
}