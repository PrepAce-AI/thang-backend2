package backend.service;

import backend.entity.Notification;
import backend.entity.Course;
import backend.entity.User;
import backend.entity.ViolationReport;
import backend.repository.CourseRepository;
import backend.repository.UserRepository;
import backend.repository.NotificationRepository;
import backend.repository.ViolationReportRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final ViolationReportRepository violationRepository;

    // Constructor injection đầy đủ 4 Repository phục vụ quản trị
    public AdminService(CourseRepository courseRepository,
                        UserRepository userRepository,
                        NotificationRepository notificationRepository,
                        ViolationReportRepository violationRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.violationRepository = violationRepository;
    }

    // ==================== COURSES ====================
    public List<Course> getAllCourse(){
        return courseRepository.findAll();
    }

    @Transactional
    public Course updateCourseStatus(Integer id, String status, String note) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));

        course.setStatus(status); // APPROVED hoặc REJECTED
        course.setReviewNote(note);

        courseRepository.save(course);
        courseRepository.flush();

        return course;
    }

    // Xóa hoàn toàn khóa học vĩnh viễn (Hard Delete)
    @Transactional
    public boolean deleteCourseById(Integer courseId) {
        if (courseRepository.existsById(courseId)) {
            courseRepository.deleteById(courseId);
            return true;
        }
        return false;
    }

    // ==================== USERS ====================
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User updateUserStatus(Integer userId, String status){
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng !!!"));
        user.setAccountStatus(status); // ACTIVE hoặc BANNED
        return userRepository.save(user);
    }

    // Xóa hoàn toàn người dùng vĩnh viễn (Hard Delete)
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

    // ==================== NOTIFICATIONS ====================
    public List<Notification> getAllNotifications() {
        return notificationRepository.findTop10ByOrderByCreatedAtDesc();
    }

    // ==================== VIOLATIONS MANAGEMENT ====================
    // Lấy danh sách toàn bộ báo cáo vi phạm học liệu từ người dùng gửi lên
    @Transactional(readOnly = true)
    public List<ViolationReport> getAllViolations() {
        return violationRepository.findAll();
    }

    // Đưa ra quyết định xử lý hồ sơ báo cáo (RESOLVED_BAN hoặc DISMISSED)
    @Transactional
    public ViolationReport handleViolation(Integer reportId, String status) {
        ViolationReport report = violationRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo vi phạm với ID: " + reportId));

        report.setStatus(status);
        return violationRepository.save(report);
    }
}