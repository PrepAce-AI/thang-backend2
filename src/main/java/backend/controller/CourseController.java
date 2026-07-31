package backend.controller;

import backend.dto.response.CourseDetailResponse;
import backend.entity.Course;
import backend.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "*")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private backend.repository.CourseRepository courseRepository;

    // Xóa dữ liệu seed mẫu
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
    public ResponseEntity<Map<String, Object>> createCourse(@RequestBody Map<String, Object> body) {
        Course course = new Course();
        course.setTitle((String) body.getOrDefault("title", "Khóa học nháp (Chưa đặt tên)"));
        course.setDescription((String) body.getOrDefault("description", ""));
        course.setIsPublished(false);
        if (body.containsKey("price")) {
            try {
                course.setPrice(new java.math.BigDecimal(String.valueOf(body.get("price"))));
            } catch (Exception e) {
                course.setPrice(java.math.BigDecimal.ZERO);
            }
        } else {
            course.setPrice(java.math.BigDecimal.ZERO);
        }

        if (body.containsKey("teacher_id")) {
            try {
                course.setTeacherId(Integer.parseInt(String.valueOf(body.get("teacher_id"))));
            } catch (Exception e) {}
        }

        if (body.containsKey("subjectId")) {
            try {
                course.setSubjectId(Integer.parseInt(String.valueOf(body.get("subjectId"))));
            } catch (Exception e) {}
        }

        if (body.containsKey("categoryId")) {
            try {
                course.setCategoryId(Integer.parseInt(String.valueOf(body.get("categoryId"))));
            } catch (Exception e) {}
        }

        Course saved = courseService.saveCourse(course);

        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", saved.getCourseId());
        response.put("title", saved.getTitle());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{courseId}")
    public ResponseEntity<?> updateCourse(@PathVariable Integer courseId, @RequestBody Map<String, Object> body) {
        Course saved = courseService.updateCourse(courseId, body);
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", saved.getCourseId());
        response.put("title", saved.getTitle());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<?> deleteCourse(@PathVariable Integer courseId) {
        courseService.deleteCourse(courseId);
        return ResponseEntity.ok().build();
    }

    @Autowired
    private backend.repository.EnrollmentRepository enrollmentRepository;
    @Autowired
    private backend.service.JwtService jwtService;
    @Autowired
    private backend.repository.UserRepository userRepository;

    @GetMapping("/{courseId}/check-enrollment")
    public ResponseEntity<Map<String, Boolean>> checkEnrollment(@PathVariable Integer courseId, @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return ResponseEntity.ok(Map.of("isEnrolled", false));
            }
            String jwt = token.replace("Bearer ", "");
            String email = jwtService.extractUsername(jwt);
            backend.entity.User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.ok(Map.of("isEnrolled", false));
            }

            // 🔥 ĐẶC QUYỀN ADMIN: Kiểm tra trực tiếp bằng kiểu Integer & RoleName
            boolean isAdmin = (user.getRoleName() != null && "ADMIN".equalsIgnoreCase(user.getRoleName()))
                    || (user.getRoleId() == 1);

            if (isAdmin) {
                return ResponseEntity.ok(Map.of("isEnrolled", true));
            }

            boolean isEnrolled = enrollmentRepository.existsByStudentIdAndCourseId(user.getId(), courseId);
            return ResponseEntity.ok(Map.of("isEnrolled", isEnrolled));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("isEnrolled", false));
        }
    }
}