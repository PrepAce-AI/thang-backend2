package backend.controller;
import backend.service.TeacherService;

import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherController {
    private final TeacherService teacherService;

    /**
     * Lấy danh sách khóa học của teacher
     * FE: GET /api/teacher/courses
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(
            @RequestHeader("X-Teacher-Id") Integer teacherId) {

        return ResponseEntity.ok(
                teacherService.getDashboard(teacherId)
        );
    }
}
