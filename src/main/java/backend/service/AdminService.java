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


// 🔥 TỰ ĐỘNG GỬI THÔNG BÁO CHO USER GỬI ĐƠN (Đã bù đủ trường chống lỗi 500)
        try {
            Notification notification = new Notification();

            // 1. Gán ID người nhận (Khớp với trường receiverId trong Entity của bạn)
            notification.setReceiverId(report.getReporterId());

            // 2. Gán tiêu đề công việc (Bắt buộc, không được để null)
            notification.setTitle("Phản hồi đơn tố cáo vi phạm");

            // 3. Gán vai trò đích nhận thông báo (Bắt buộc, không được để null)
            notification.setTargetRole("STUDENT");

            // 4. Gán thời gian tạo
            notification.setCreatedAt(new Date());

            // 5. Gán ID người tạo thông báo (Admin hệ thống - mặc định truyền ID là 1 hoặc lấy từ Token)
            notification.setCreatedBy(1);

            // Cấu trúc nội dung lời nhắn gửi đi
            String msg = status.equalsIgnoreCase("RESOLVED_BAN")
                    ? "Thành công: Đơn tố cáo của bạn về '" + report.getReportedTarget() + "' đã được xử lý. Đối tượng vi phạm đã bị xử phạt. Phản hồi từ Admin: " + adminNote
                    : "Phản hồi đơn tố cáo: Đơn tố cáo của bạn về '" + report.getReportedTarget() + "' đã bị từ chối/bác bỏ do chưa đủ bằng chứng. Lý do: " + adminNote;

            notification.setContent(msg);

            // Thực thi lưu xuống Database
            notificationRepository.save(notification);
            System.out.println("✅ Tự động bắn thông báo phản hồi vi phạm thành công tới User #" + report.getReporterId());
        } catch (Exception e) {
            // Nếu có lỗi ngầm phát sinh ở tầng DB, log ra để theo dõi tránh làm gãy luồng xử lý chính
            System.err.println("❌ Lỗi nghiêm trọng khi lưu thông báo vi phạm: " + e.getMessage());
            e.printStackTrace();
        }

        return report;
    }

    // 🔥 THÊM HÀM CHO NGƯỜI DÙNG GỬI ĐƠN TỐ CÁO MỚI
    @Transactional
    public ViolationReport createViolationReport(ViolationReport newReport) {
        newReport.setStatus("PENDING");
        newReport.setCreatedAt(new Date());
        return violationRepository.save(newReport);
    }
}