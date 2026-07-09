package backend.controller;

import backend.entity.Course;
import backend.entity.User;
import backend.service.AdminService;
import backend.entity.Notification;
import backend.service.JwtService;
import backend.service.NotificationService;
import backend.entity.ViolationReport;

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
        Notification noti = notificationService.createNotification(title, content, targetRole, adminId);
        return ResponseEntity.ok(noti);
    }

    // ====================== VIOLATIONS MANAGEMENT (TASK 43) ======================
    // API lấy toàn bộ danh sách báo cáo vi phạm học liệu để Admin kiểm tra
    @GetMapping("/violations")
    public ResponseEntity<List<ViolationReport>> getAllViolations() {
        return ResponseEntity.ok(adminService.getAllViolations());
    }

    // API đưa ra quyết định xử lý báo cáo vi phạm (RESOLVED_BAN, DISMISSED)
    @PutMapping("/violations/{id}")
    public ResponseEntity<ViolationReport> handleViolation(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        ViolationReport report = adminService.handleViolation(id, status);
        return ResponseEntity.ok(report);
    }
}