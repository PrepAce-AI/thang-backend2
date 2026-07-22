package backend.service;

import backend.entity.Notification;
import backend.entity.Course;
import backend.entity.User;
import backend.entity.ViolationReport;
import backend.entity.UserActivity;

import backend.repository.UserActivityRepository;
import backend.repository.CourseRepository;
import backend.repository.UserRepository;
import backend.repository.NotificationRepository;
import backend.repository.ViolationReportRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class AdminService {
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final ViolationReportRepository violationRepository;
    private final UserActivityRepository userActivityRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public AdminService(CourseRepository courseRepository,
                        UserRepository userRepository,
                        NotificationRepository notificationRepository,
                        ViolationReportRepository violationRepository,
                        UserActivityRepository userActivityRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.violationRepository = violationRepository;
        this.userActivityRepository = userActivityRepository;
    }

    // ==================== COURSES ====================
    public List<Course> getAllCourse(){
        return courseRepository.findAll();
    }

    @Transactional
    public Course updateCourseStatus(Integer id, String status, String note) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học với ID: " + id));

        course.setStatus(status); // APPROVED, PUBLISHED, REJECTED, PENDING
        if (note != null) {
            course.setReviewNote(note);
        }

        courseRepository.save(course);
        courseRepository.flush();

        // 🔥 TỰ ĐỘNG BẮN THÔNG BÁO CHO GIÁO VIÊN KHI ĐỔI TRẠNG THÁI KHÓA HỌC
        try {
            Integer teacherId = course.getTeacherId();
            if (teacherId != null) {
                Notification notification = new Notification();
                notification.setReceiverId(teacherId);
                notification.setTargetRole("TEACHER");
                notification.setCreatedAt(new Date());
                notification.setCreatedBy(1); // Admin

                String title = "";
                String content = "";

                if ("PUBLISHED".equalsIgnoreCase(status) || "APPROVED".equalsIgnoreCase(status)) {
                    title = "🎉 Khóa học đã được phê duyệt";
                    content = "Chúc mừng! Khóa học \"" + course.getTitle() + "\" của bạn đã được Ban quản trị phê duyệt và xuất bản công khai.";
                } else if ("REJECTED".equalsIgnoreCase(status)) {
                    title = "✏️ Yêu cầu chỉnh sửa khóa học";
                    content = "Khóa học \"" + course.getTitle() + "\" cần chỉnh sửa nội dung. Lý do: \"" + (note != null ? note.trim() : "Chưa nhập lý do") + "\"";
                } else if ("PENDING".equalsIgnoreCase(status)) {
                    title = "⚠️ Khóa học bị hạ trạng thái";
                    content = "Khóa học \"" + course.getTitle() + "\" đã bị hạ xuống trạng thái Chờ duyệt. Lý do: \"" + (note != null ? note.trim() : "Chưa nhập lý do") + "\"";
                }

                if (!title.isEmpty()) {
                    notification.setTitle(title);
                    notification.setContent(content);
                    notificationRepository.save(notification);
                    System.out.println("✅ Đã tự động bắn thông báo cập nhật trạng thái tới Giáo viên #" + teacherId);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi tự động tạo thông báo: " + e.getMessage());
        }

        return course;
    }

    // 🔥 TỰ ĐỘNG BẮN THÔNG BÁO KHI XÓA KHÓA HỌC
    @Transactional
    public boolean deleteCourseById(Integer courseId, String reason) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course != null) {
            Integer teacherId = course.getTeacherId();
            if (teacherId != null) {
                try {
                    Notification notification = new Notification();
                    notification.setReceiverId(teacherId);
                    notification.setTitle("🗑️ Khóa học đã bị gỡ bỏ");
                    notification.setContent("Khóa học \"" + course.getTitle() + "\" của bạn đã bị gỡ khỏi hệ thống. Lý do: \"" + (reason != null && !reason.isBlank() ? reason.trim() : "Theo quy định hệ thống") + "\"");
                    notification.setTargetRole("TEACHER");
                    notification.setCreatedAt(new Date());
                    notification.setCreatedBy(1); // Admin
                    notificationRepository.save(notification);
                    System.out.println("✅ Đã bắn thông báo xóa khóa học tới Giáo viên #" + teacherId);
                } catch (Exception e) {
                    System.err.println("❌ Lỗi khi gửi thông báo xóa khóa học: " + e.getMessage());
                }
            }
            courseRepository.delete(course);
            return true;
        }
        return false;
    }

    // Overload giữ nguyên tương thích cũ
    @Transactional
    public boolean deleteCourseById(Integer courseId) {
        return deleteCourseById(courseId, null);
    }

    // ==================== USERS ====================
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User updateUserStatus(Integer userId, String status){
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng !!!"));
        user.setAccountStatus(status);
        return userRepository.save(user);
    }

    @Transactional
    public boolean deleteUser(Integer userId) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
            return true;
        }
        return false;
    }

    // ==================== STATISTICS ====================
    public Map<String, Long> getDashboardStats(){
        long totalUsers = userRepository.count();
        long totalCourses = courseRepository.count();
        long publishedCourses = courseRepository.findAll().stream()
                .filter(c -> "PUBLISHED".equals(c.getStatus()) || "APPROVED".equals(c.getStatus()))
                .count();

        return Map.of("totalUsers", totalUsers,
                "totalCourses", totalCourses,
                "publishedCourses", publishedCourses);
    }

    // ==================== VIOLATIONS MANAGEMENT ====================
    @Transactional(readOnly = true)
    public List<ViolationReport> getAllViolations() {
        return violationRepository.findAll();
    }

    @Transactional
    public ViolationReport handleViolation(Integer reportId, String status, String adminNote) {
        ViolationReport report = violationRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo vi phạm với ID: " + reportId));

        report.setStatus(status);
        report.setAdminNote(adminNote);
        violationRepository.save(report);

        try {
            Notification notification = new Notification();
            notification.setReceiverId(report.getReporterId());
            notification.setTitle("Phản hồi đơn tố cáo vi phạm");
            notification.setTargetRole("STUDENT");
            notification.setCreatedAt(new Date());
            notification.setCreatedBy(1);

            String msg = status.equalsIgnoreCase("RESOLVED_BAN")
                    ? "Thành công: Đơn tố cáo của bạn về '" + report.getReportedTarget() + "' đã được xử lý. Đối tượng vi phạm đã bị xử phạt. Phản hồi từ Admin: " + adminNote
                    : "Phản hồi đơn tố cáo: Đơn tố cáo của bạn về '" + report.getReportedTarget() + "' đã bị từ chối/bác bỏ do chưa đủ bằng chứng. Lý do: " + adminNote;

            notification.setContent(msg);
            notificationRepository.save(notification);
            System.out.println("✅ Tự động bắn thông báo phản hồi vi phạm thành công tới User #" + report.getReporterId());
        } catch (Exception e) {
            System.err.println("❌ Lỗi nghiêm trọng khi lưu thông báo vi phạm: " + e.getMessage());
            e.printStackTrace();
        }

        return report;
    }

    @Transactional
    public ViolationReport createViolationReport(ViolationReport newReport) {
        newReport.setStatus("PENDING");
        newReport.setCreatedAt(new Date());
        return violationRepository.save(newReport);
    }

    // ==================== TEACHER REQUESTS & ROLE MANAGEMENT ====================

    @Transactional
    public User requestToBecomeTeacher(Integer userId, String education, String experience, String proofUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        user.setTeacherRequestStatus("PENDING");
        user.setEducation(education);

        // Nối URL minh chứng Cloudinary vào trường kinh nghiệm hoặc tiểu sử
        if (proofUrl != null && !proofUrl.trim().isEmpty()) {
            user.setExperience(experience + "\n\n📎 Minh chứng năng lực: " + proofUrl.trim());
        } else {
            user.setExperience(experience);
        }

        return userRepository.save(user);
    }

    @Transactional
    public User handleTeacherRequest(Integer userId, String decision) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if ("APPROVE".equalsIgnoreCase(decision)) {
            user.setTeacherRequestStatus("APPROVED");
            user.setRoleId(2);
            user.setRoleName("TEACHER");
        } else {
            user.setTeacherRequestStatus("REJECTED");
        }
        return userRepository.save(user);
    }

    @Transactional
    public User changeUserRole(Integer userId, Integer newRoleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        user.setRoleId(newRoleId);
        String roleName = newRoleId == 1 ? "ADMIN" : newRoleId == 2 ? "TEACHER" : "STUDENT";
        user.setRoleName(roleName);

        User savedUser = userRepository.save(user);

        entityManager.flush();
        entityManager.clear();

        return savedUser;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserDetailWithLogs(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        List<UserActivity> activities = userActivityRepository.findByUserIdOrderByTimestampDesc(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("education", user.getEducation());
        response.put("experience", user.getExperience());
        response.put("activities", activities);

        return response;
    }

    @Transactional
    public void saveUserActivity(Integer userId, String action) {
        UserActivity log = new UserActivity();
        log.setUserId(userId);
        log.setAction(action);
        log.setTimestamp(java.time.LocalDateTime.now());

        userActivityRepository.save(log);
    }

    @Transactional
    public backend.entity.User createUser(backend.entity.User newUser) {
        if (userRepository.existsByEmail(newUser.getEmail())) {
            throw new RuntimeException("Email này đã được sử dụng trên hệ thống!");
        }

        if (newUser.getRoleId() == 1) newUser.setRoleName("ADMIN");
        else if (newUser.getRoleId() == 2) newUser.setRoleName("TEACHER");
        else newUser.setRoleName("STUDENT");

        newUser.setAccountStatus("ACTIVE");
        newUser.setCreatedAt(new java.util.Date());

        return userRepository.save(newUser);
    }
}