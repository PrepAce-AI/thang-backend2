package backend.service;

import backend.dto.request.ReviewRequest;
import backend.dto.response.ReviewResponse;
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

    @Transactional
    public ReviewResponse createReview(Integer courseId, ReviewRequest request) {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        CourseReview review = new CourseReview();
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setUser(user);

        Course course = new Course();
        course.setId(courseId);
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
        return reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId)
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
}