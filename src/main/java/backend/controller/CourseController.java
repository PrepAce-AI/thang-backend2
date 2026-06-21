package backend.controller;

import backend.dto.response.CourseDetailResponse;
import backend.entity.Course;
import backend.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List; // 🔥 ĐÃ THÊM: Import thư viện List để trả về danh sách
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private backend.repository.CourseRepository courseRepository;

    @GetMapping("/delete-seed")
    public ResponseEntity<String> deleteSeedData() {
        try {
            java.util.List<backend.entity.Course> courses = courseRepository.findAll();
            for (backend.entity.Course c : courses) {
                if (c.getTitle().contains("THPT Quốc Gia") || 
                    c.getTitle().contains("Vật Lý 12") || 
                    c.getTitle().contains("Hóa Học 12")) {
                    courseRepository.delete(c);
                }
            }
            return ResponseEntity.ok("Đã xóa sạch các khóa học mẫu THPT!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi khi xóa: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<backend.dto.response.CourseListResponse>> getAllCourses() {
        List<backend.dto.response.CourseListResponse> courses = courseService.getAllCourses();
        return ResponseEntity.ok(courses);
    }
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseDetailResponse> getCourseDetail(@PathVariable Integer courseId) {
        CourseDetailResponse response = courseService.getCourseDetailById(courseId);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<java.util.Map<String, Object>> createCourse(@RequestBody java.util.Map<String, Object> body) {
        Course course = new Course();
        course.setTitle((String) body.getOrDefault("title", "Khóa học nháp (Chưa đặt tên)"));
        course.setDescription((String) body.getOrDefault("description", ""));
        course.setIsPublished(false);
        course.setPrice(java.math.BigDecimal.ZERO);
        
        if (body.containsKey("teacher_id")) {
            try {
                course.setTeacherId(Integer.parseInt(String.valueOf(body.get("teacher_id"))));
            } catch (Exception e) {}
        }
        
        // Gán cứng một subject_id (ví dụ 1) để lọt qua cổng kiểm duyệt NOT NULL của SQL Server
        course.setSubjectId(1);


        Course saved = courseService.saveCourse(course); // Tạm gọi qua repository hoặc service
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", saved.getId());
        response.put("title", saved.getTitle());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{courseId}")
    public ResponseEntity<?> updateCourse(@PathVariable Integer courseId, @RequestBody java.util.Map<String, Object> body) {
        Course saved = courseService.updateCourse(courseId, body);
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", saved.getId());
        response.put("title", saved.getTitle());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<?> deleteCourse(@PathVariable Integer courseId) {
        courseService.deleteCourse(courseId);
        return ResponseEntity.ok().build();
    }
}
