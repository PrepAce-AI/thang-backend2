package backend.controller;

import backend.dto.response.CourseDetailResponse;
import backend.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseDetailResponse> getCourseDetail(@PathVariable Integer courseId) {
        CourseDetailResponse response = courseService.getCourseDetailById(courseId);
        return ResponseEntity.ok(response);
    }
}