package backend.controller;

import backend.entity.Course;
import backend.entity.User;
import backend.service.AdminService;
import backend.entity.Notification;
import backend.service.JwtService;
import backend.service.NotificationService;
import backend.entity.ViolationReport;

// 🔥 ĐÃ THÊM: Import thư viện HttpStatus để giải quyết lỗi biên dịch màu đỏ ở cuối file
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AdminController {

    private final AdminService adminService;
    private final NotificationService notificationService;
    private final JwtService jwtService;

    public AdminController(AdminService adminService, NotificationService notificationService, JwtService jwtService) {
        this.adminService = adminService;
        this.notificationService = notificationService;
        this.jwtService = jwtService;
    }

    // ====================== AUTHENTICATION HELPER ======================
    private Integer getCurrentAdminId(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            String jwt = token.replace("Bearer ", "");
            return jwtService.extractUserId(jwt);
        }
        return 1; // ID Admin mặc định dự phòng nếu chạy test local không có token
    }

    // ====================== DASHBOARD (TASK 44) ======================
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        System.out.println("✅ ADMIN STATS ENDPOINT ĐÃ ĐƯỢC GỌI!");
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // ====================== COURSES MANAGEMENT (TASK 42) ======================
    @GetMapping("/courses")
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(adminService.getAllCourse());
    }

    // Phê duyệt hoặc Yêu cầu sửa khóa học (APPROVED, REJECTED)
    @PatchMapping("/courses/{id}/status")
    public ResponseEntity<Course> updateCourseStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        String note = request.get("note");
        Course updated = adminService.updateCourseStatus(id, status, note);
        return ResponseEntity.ok(updated);
    }

    // Xóa hoàn toàn khóa học vĩnh viễn khỏi hệ thống
    @DeleteMapping("/courses/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable Integer id) {
        boolean isDeleted = adminService.deleteCourseById(id);
        if (isDeleted) {
            return ResponseEntity.ok(Map.of("message", "Đã xóa hoàn toàn khóa học khỏi hệ thống thành công!"));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Xóa khóa học thất bại hoặc ID không tồn tại."));
    }

    // ====================== USERS MANAGEMENT (TASK 40) ======================
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    // Tạm khóa (BANNED) hoặc Mở khóa (ACTIVE) tài khoản người dùng
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<User> updateUserStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        User updated = adminService.updateUserStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    // Xóa hoàn toàn tài khoản người dùng khỏi cơ sở dữ liệu (Hard Delete)
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Integer id) {
        boolean isDeleted = adminService.deleteUser(id);
        if (isDeleted) {
            return ResponseEntity.ok(Map.of("message", "Đã xóa hoàn toàn người dùng khỏi hệ thống thành công!"));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Xóa người dùng thất bại hoặc ID không tồn tại."));
    }

    // ====================== NOTIFICATIONS (TASK 45) ======================
    @GetMapping("/notifications")
    public ResponseEntity<List<Notification>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    @PostMapping("/notifications")
    public ResponseEntity<Notification> createNotification(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody Map<String, String> request) {
        String title = request.get("title");
        String content = request.get("content");
        String targetRole = request.get("targetRole"); // ALL, STUDENT, TEACHER

        Integer adminId = getCurrentAdminId(token);
        Notification noti = notificationService.createNotification(title, content, targetRole, 1,null);
        return ResponseEntity.ok(noti);
    }

    // ====================== VIOLATIONS MANAGEMENT (TASK 43) ======================
    @GetMapping("/violations")
    public ResponseEntity<List<ViolationReport>> getAllViolations() {
        return ResponseEntity.ok(adminService.getAllViolations());
    }

    @PutMapping("/violations/{id}")
    public ResponseEntity<?> handleViolation(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        String adminNote = body.get("adminNote");

        if (adminNote == null || adminNote.trim().isEmpty()) {
            adminNote = "Đã xử lý theo quy chuẩn cộng đồng.";
        }

        ViolationReport updated = adminService.handleViolation(id, status, adminNote);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/violations/submit")
    public ResponseEntity<?> submitViolation(@RequestBody ViolationReport report) {
        ViolationReport saved = adminService.createViolationReport(report);
        return ResponseEntity.ok(saved);
    }

    // ====================== TEACHER REQUESTS & ROLES ======================

    // 🔥 CẬP NHẬT: Tiếp nhận thông tin học vấn và kinh nghiệm từ `@RequestBody` của học sinh gửi lên
    @PostMapping("/users/{id}/request-teacher")
    public ResponseEntity<?> requestTeacher(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String education = body.get("education");
        String experience = body.get("experience");
        return ResponseEntity.ok(adminService.requestToBecomeTeacher(id, education, experience));
    }

    // Admin xử lý duyệt đơn ứng tuyển giáo viên
    @PutMapping("/users/{id}/review-teacher")
    public ResponseEntity<?> reviewTeacher(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String decision = body.get("decision"); // "APPROVE" hoặc "REJECT"
        return ResponseEntity.ok(adminService.handleTeacherRequest(id, decision));
    }

    // Admin chủ động đổi vai trò trực tiếp của User từ Select-Box
    @PutMapping("/users/{id}/change-role")
    public ResponseEntity<?> changeRole(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        Integer newRoleId = body.get("roleId"); // 1: ADMIN, 2: TEACHER, 3: STUDENT
        return ResponseEntity.ok(adminService.changeUserRole(id, newRoleId));
    }

    // 🔥 Lấy thông tin chi tiết kèm danh sách nhật ký log hoạt động của người dùng
    @GetMapping("/users/{id}/activity")
    public ResponseEntity<?> getUserActivityLog(@PathVariable Integer id) {
        try {
            Map<String, Object> data = adminService.getUserDetailWithLogs(id);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi khi lấy nhật ký hoạt động: " + e.getMessage()));
        }
    }
    // 🔥 API MỚI: Tiếp nhận và lưu lại hoạt động thực tế của người dùng
    @PostMapping("/users/{id}/activity")
    public ResponseEntity<?> saveUserActivity(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        try {
            String actionText = body.get("action");

            // Gọi qua Service xử lý thay vì gọi trực tiếp Repository
            adminService.saveUserActivity(id, actionText);

            return ResponseEntity.ok(Map.of("message", "Ghi nhận hoạt động thành công!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi lưu log: " + e.getMessage()));
        }
    }
}