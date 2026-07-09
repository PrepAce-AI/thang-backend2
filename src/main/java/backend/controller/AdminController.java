package backend.controller;
import backend.entity.Course;
import backend.entity.User;
import backend.service.AdminService;
import backend.entity.Notification;
import backend.service.JwtService;
import backend.service.NotificationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
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
        String jwt = token.replace("Bearer ", "");
        return jwtService.extractUserId(jwt);
    }

    // ====================== DASHBOARD ======================
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats(){
        System.out.println("✅ ADMIN STATS ENDPOINT ĐÃ ĐƯỢC GỌI!");
        return ResponseEntity.ok(adminService.getDashboardStats());
    }

    // ====================== COURSES ======================
    @GetMapping("/courses")
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(adminService.getAllCourse());
    }

    @PatchMapping("/courses/{id}/status")
    public ResponseEntity<Course> updateCourseStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {

        String status = request.get("status");
        String note = request.get("note");

        Course updated = adminService.updateCourseStatus(id, status, note);
        return ResponseEntity.ok(updated);
    }

    // ====================== USERS ======================
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<User> updateUserStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {

        String status = request.get("status");
        User updated = adminService.updateUserStatus(id, status);
        return ResponseEntity.ok(updated);
    }

    // ====================== NOTIFICATIONS ======================

    @GetMapping("/notifications")
    public ResponseEntity<List<Notification>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    @PostMapping("/notifications")
    public ResponseEntity<Notification> createNotification(@RequestBody Map<String, String> request) {
        String title = request.get("title");
        String content = request.get("content");
        String targetRole = request.get("targetRole"); // ALL, STUDENT, TEACHER

        // Lấy userId của Admin hiện tại (từ JWT)
        // Tạm thời hardcode createdBy = 1 (Admin), sau sẽ lấy từ token
        Notification noti = notificationService.createNotification(title, content, targetRole, 1,null);
        return ResponseEntity.ok(noti);
    }
}
