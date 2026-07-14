package backend.controller;

import backend.dto.response.CourseListResponse;
import backend.service.EnrollmentService;
import backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<List<CourseListResponse>> getMyEnrolledCourses(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build(); // Trả về Unauthorized nếu chưa đăng nhập / thiếu token
        }

        // 1. Lấy Email của học sinh đang đăng nhập từ Token bảo mật
        String email = authentication.getName();

        // 2. Tìm ID của học sinh tương ứng với Email đó dưới DB
        Integer studentId = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng có Email: " + email))
                .getId(); // Sử dụng getId() khớp với thực tế User của nhóm ông

        // 3. Gọi Service bốc danh sách khóa học và trả về cho React
        List<CourseListResponse> myCourses = enrollmentService.getMyEnrolledCourses(studentId);
        return ResponseEntity.ok(myCourses);
    }
}