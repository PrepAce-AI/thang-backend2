package backend.controller;

import backend.dto.request.ReviewRequest;
import backend.dto.response.ReviewResponse;
import backend.dto.response.ReviewSummaryResponse;
import backend.service.CourseReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses/{courseId}/reviews")
public class CourseReviewController {

    @Autowired
    private CourseReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> addReview(
            org.springframework.security.core.Authentication authentication,
            @PathVariable Integer courseId,
            @RequestBody ReviewRequest request) {
        String email = authentication.getName();
        return ResponseEntity.ok(reviewService.createReview(email, courseId, request));
    }

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable Integer courseId) {
        return ResponseEntity.ok(reviewService.getReviewsByCourse(courseId));
    }
    // 🔥 ĐÃ THÊM: Endpoint tóm tắt thống kê khớp chính xác với dòng gọi từ CourseDetailPage.jsx
    @GetMapping("/summary")
    public ResponseEntity<ReviewSummaryResponse> getReviewSummary(@PathVariable Integer courseId) {
        return ResponseEntity.ok(reviewService.getReviewSummary(courseId));
    }
}