package backend.controller;

import backend.dto.request.ReviewRequest;
import backend.dto.response.ReviewResponse;
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
            @PathVariable Integer courseId,
            @RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.createReview(courseId, request));
    }

    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable Integer courseId) {
        return ResponseEntity.ok(reviewService.getReviewsByCourse(courseId));
    }
}