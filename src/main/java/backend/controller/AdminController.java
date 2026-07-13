package backend.controller;

import backend.entity.Course;
import backend.entity.User;
import backend.service.AdminService;
import backend.entity.Notification;
import backend.service.JwtService;
import backend.service.NotificationService;
import backend.entity.ViolationReport;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    private backend.repository.CategoryRepository categoryRepository;

    @Autowired
    private backend.repository.CourseRepository courseRepository;

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
        return 1;
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

    @PatchMapping("/courses/{id}/status")
    public ResponseEntity<Course> updateCourseStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {
        String status = request.get("status");
        String note = request.get("note");
        Course updated = adminService.updateCourseStatus(id, status, note);
        return ResponseEntity.ok(updated);
    }

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

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {

        String status = request.get("status");
        String reason = request.get("reason");

        if (id != null && id == 1) {
            if ("BANNED".equalsIgnoreCase(status) || "DEACTIVATED".equalsIgnoreCase(status)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "message", "❌ Chặn bảo mật (BR-UC40-02): Đây là tài khoản Quản trị viên tối cao của hệ thống PrepAce. Không thể tự khóa hoặc vô hiệu hóa!"
                ));
            }
        }

        if ("BANNED".equalsIgnoreCase(status)) {
            if (reason == null || reason.trim().length() < 20) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "message", "❌ Thất bại (BR-UC40-01): Bạn bắt buộc phải nhập lý do khóa tài khoản chi tiết từ 20 ký tự trở lên!"
                ));
            }
        }

        User updated = adminService.updateUserStatus(id, status);

        if ("BANNED".equalsIgnoreCase(status) && updated != null) {
            try {
                String logMessage = "Tài khoản bị khóa đăng nhập hệ thống. Lý do cụ thể: " + reason.trim();
                adminService.saveUserActivity(id, logMessage);
                System.out.println("🎉 Đã lưu lý do khóa thành công vào Audit Log cho User ID: " + id);
            } catch (Exception logErr) {
                System.out.println("⚠️ Lỗi kích hoạt lưu log tự động: " + logErr.getMessage());
            }
        }

        return ResponseEntity.ok(updated);
    }

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
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> request) {

        String title = request.get("title");
        String content = request.get("content");
        String targetRole = request.get("targetRole");

        Integer adminId = getCurrentAdminId(token);

        Notification noti = notificationService.createNotification(
                title,
                content,
                targetRole,
                adminId,
                null
        );

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

    @PostMapping("/users/{id}/request-teacher")
    public ResponseEntity<?> requestTeacher(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String education = body.get("education");
        String experience = body.get("experience");
        return ResponseEntity.ok(adminService.requestToBecomeTeacher(id, education, experience));
    }

    @PutMapping("/users/{id}/review-teacher")
    public ResponseEntity<?> reviewTeacher(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        String decision = body.get("decision");
        Object result = adminService.handleTeacherRequest(id, decision);

        // 🔥 THÊM: Nếu duyệt thành công duyệt làm giáo viên, tự động đồng bộ role_name lên 'TEACHER'
        if ("APPROVED".equalsIgnoreCase(decision)) {
            try {
                jdbcTemplate.update("UPDATE Users SET role_name = 'TEACHER' WHERE user_id = ?", id);
                System.out.println("🎉 Đã tự động đồng bộ thành công role_name thành TEACHER sau khi duyệt đơn!");
            } catch (Exception e) {
                System.out.println("⚠️ Lỗi đồng bộ chuỗi chữ role_name khi duyệt đơn: " + e.getMessage());
            }
        }
        return ResponseEntity.ok(result);
    }

    @PutMapping("/users/{id}/change-role")
    public ResponseEntity<?> changeRole(@PathVariable Integer id, @RequestBody Map<String, Integer> body) {
        Integer newRoleId = body.get("roleId");

        if (id != null && id == 1 && newRoleId != 1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", "❌ Chặn phân quyền (BR-UC40-02): Đây là tài khoản Quản trị viên tối cao của hệ thống. Bạn không thể hạ vai trò quyền hạn!"
            ));
        }

        Object result = adminService.changeUserRole(id, newRoleId);

        // 🔥 THÊM: Đồng bộ cứng lại trường role_name bằng JdbcTemplate để tránh lệch pha dữ liệu chữ
        try {
            String targetRoleName = (newRoleId == 2) ? "TEACHER" : (newRoleId == 3) ? "STUDENT" : "ADMIN";
            jdbcTemplate.update("UPDATE Users SET role_name = ? WHERE user_id = ?", targetRoleName, id);
            System.out.println("🎉 Đã đồng bộ role_name thành: " + targetRoleName + " cho User ID: " + id);
        } catch (Exception e) {
            System.out.println("⚠️ Lỗi đồng bộ chuỗi chữ role_name: " + e.getMessage());
        }

        return ResponseEntity.ok(result);
    }

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

    @PostMapping("/users/{id}/activity")
    public ResponseEntity<?> saveUserActivity(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        try {
            String actionText = body.get("action");
            adminService.saveUserActivity(id, actionText);
            return ResponseEntity.ok(Map.of("message", "Ghi nhận hoạt động thành công!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi lưu log: " + e.getMessage()));
        }
    }

    @PostMapping("/users")
    public ResponseEntity<?> addNewUser(@RequestBody backend.entity.User userRequest) {
        try {
            backend.entity.User savedUser = adminService.createUser(userRequest);
            try {
                adminService.saveUserActivity(savedUser.getId(),
                        "Admin khởi tạo tài khoản mới: " + savedUser.getFullName() + " (" + savedUser.getRoleName() + ")");
            } catch (Exception logErr) {
                System.out.println("Lỗi ghi nhận log tự động: " + logErr.getMessage());
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Thêm người dùng mới thành công!",
                    "user", savedUser
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi máy chủ hệ thống: " + e.getMessage()));
        }
    }

    // Lấy toàn bộ danh mục (Dành cho màn hình Admin)
    @GetMapping("/categories/all")
    public ResponseEntity<List<backend.entity.Category>> getAllCategoriesForAdmin() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    // Lấy danh mục đang hoạt động (Dành cho Giáo viên / Học sinh mới)
    @GetMapping("/categories/active")
    public ResponseEntity<List<backend.entity.Category>> getActiveCategories() {
        return ResponseEntity.ok(categoryRepository.findByIsHiddenFalse());
    }

    // Tạo mới danh mục (Xử lý Exception E-01: Trùng tên)
    @PostMapping("/categories")
    public ResponseEntity<?> createCategory(@RequestBody Map<String, String> body) {
        String name = body.get("categoryName");

        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tên danh mục không được để trống!"));
        }

        if (categoryRepository.existsByCategoryNameAndIsHiddenFalse(name.trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "message", "⚠️ Cảnh báo (E-01): Tên danh mục '" + name.trim() + "' đã tồn tại trên hệ thống. Vui lòng chọn tên khác!"
            ));
        }

        backend.entity.Category category = new backend.entity.Category();
        category.setCategoryName(name.trim());
        category.setIsHidden(false);

        return ResponseEntity.ok(categoryRepository.save(category));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<?> deleteOrHideCategory(@PathVariable Integer id) {
        backend.entity.Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Không tìm thấy danh mục này."));
        }

        long coursesCount = adminService.getAllCourse().stream()
                .filter(c -> id.equals(c.getSubjectId()) || id.equals(c.getCourseId()))
                .count();

        if (coursesCount > 0) {
            category.setIsHidden(true);
            categoryRepository.save(category);
            return ResponseEntity.ok(Map.of(
                    "message", "⚠️ Thông báo (BR-UC41-01): Danh mục này đang có " + coursesCount + " khóa học sử dụng. Hệ thống đã tự động CHUYỂN SANG TRẠNG THÁI ẨN để bảo toàn lịch sử!"
            ));
        }

        categoryRepository.delete(category);
        return ResponseEntity.ok(Map.of("message", "Đã xóa hoàn toàn danh mục trống thành công!"));
    }

    // 1. API công khai bốc dữ liệu Banner lên Trang chủ
    @GetMapping("/public/ui-config/banner")
    public ResponseEntity<?> getBannerConfig() {
        try {
            String title = jdbcTemplate.queryForObject("SELECT config_value FROM SystemConfig WHERE config_key = 'banner_title'", String.class);
            String subtitle = jdbcTemplate.queryForObject("SELECT config_value FROM SystemConfig WHERE config_key = 'banner_subtitle'", String.class);
            String btnText = jdbcTemplate.queryForObject("SELECT config_value FROM SystemConfig WHERE config_key = 'banner_btn_text'", String.class);

            return ResponseEntity.ok(Map.of(
                    "title", title != null ? title : "",
                    "subtitle", subtitle != null ? subtitle : "",
                    "btnText", btnText != null ? btnText : ""
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "title", "Bứt phá điểm số cùng PrepAce AI",
                    "subtitle", "Hệ thống học tập thông minh sử dụng AI để phân tích năng lực...",
                    "btnText", "Bắt đầu ngay"
            ));
        }
    }

    // 2. API Admin cập nhật cấu hình Banner
    @PostMapping("/ui-config/banner")
    public ResponseEntity<?> saveBannerConfig(@RequestBody Map<String, String> body) {
        try {
            jdbcTemplate.update("UPDATE SystemConfig SET config_value = ? WHERE config_key = 'banner_title'", body.get("title"));
            jdbcTemplate.update("UPDATE SystemConfig SET config_value = ? WHERE config_key = 'banner_subtitle'", body.get("subtitle"));
            jdbcTemplate.update("UPDATE SystemConfig SET config_value = ? WHERE config_key = 'banner_btn_text'", body.get("btnText"));
            return ResponseEntity.ok(Map.of("message", "Đã cập nhật cấu hình Banner hệ thống thành công!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Lỗi máy chủ: " + e.getMessage()));
        }
    }
}