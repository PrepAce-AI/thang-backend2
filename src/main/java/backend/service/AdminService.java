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

import org.springframework.beans.factory.annotation.Autowired; // 🔥 ĐÃ THÊM: Import Autowired
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap; // 🔥 ĐÃ THÊM: Import HashMap để đóng gói dữ liệu JSON
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

    // 🔥 ĐÃ THÊM: Khai báo thêm Repository ghi log hoạt động
    private final UserActivityRepository userActivityRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // 🔥 CẬP NHẬT: Constructor injection đầy đủ tất cả các Repository phục vụ quản trị
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

        // TỰ ĐỘNG GỬI THÔNG BÁO CHO USER GỬI ĐƠN
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

    // 1. 🔥 CẬP NHẬT: Tiếp nhận yêu cầu làm giáo viên kèm thông tin học vấn, kinh nghiệm từ Form
    @Transactional
    public User requestToBecomeTeacher(Integer userId, String education, String experience) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        user.setTeacherRequestStatus("PENDING");
        user.setEducation(education);   // Lưu học vấn vào DB
        user.setExperience(experience); // Lưu kinh nghiệm vào DB

        return userRepository.save(user);
    }

    // 2. Admin Phê duyệt / Từ chối yêu cầu ứng tuyển giáo viên
    @Transactional
    public User handleTeacherRequest(Integer userId, String decision) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        if ("APPROVE".equalsIgnoreCase(decision)) {
            user.setTeacherRequestStatus("APPROVED");
            user.setRoleId(2); // 🔥 ĐỔI THẲNG ROLE THÀNH TEACHER (Giáo viên)
            user.setRoleName("TEACHER");
        } else {
            user.setTeacherRequestStatus("REJECTED");
        }
        return userRepository.save(user);
    }

    // 3. Admin thay đổi vai trò (Role) trực tiếp cho thành viên bất kỳ từ Select-Box
    @Transactional
    public User changeUserRole(Integer userId, Integer newRoleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        user.setRoleId(newRoleId);
        String roleName = newRoleId == 1 ? "ADMIN" : newRoleId == 2 ? "TEACHER" : "STUDENT";
        user.setRoleName(roleName);

        User savedUser = userRepository.save(user);

        // ÉP HIBERNATE ĐẨY DỮ LIỆU XUỐNG VÀ XÓA CACHE TRONG PHIÊN NÀY
        entityManager.flush();
        entityManager.clear();

        return savedUser;
    }

    // 4. 🔥 Lấy chi tiết hồ sơ người dùng kèm nhật ký hoạt động hệ thống
    @Transactional(readOnly = true)
    public Map<String, Object> getUserDetailWithLogs(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // Lấy danh sách log hoạt động từ database
        List<UserActivity> activities = userActivityRepository.findByUserIdOrderByTimestampDesc(userId);

        // Đóng gói dữ liệu trả về theo đúng định dạng JSON mà Frontend chờ sẵn
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

    // 🔥 THÊM user
    @Transactional
    public backend.entity.User createUser(backend.entity.User newUser) {
        // Kiểm tra trùng lặp Email trước khi tạo
        if (userRepository.existsByEmail(newUser.getEmail())) {
            throw new RuntimeException("Email này đã được sử dụng trên hệ thống!");
        }

        // Đồng bộ role_name tương ứng với role_id truyền lên từ Form cho khớp DB cũ của Hưng
        if (newUser.getRoleId() == 1) newUser.setRoleName("ADMIN");
        else if (newUser.getRoleId() == 2) newUser.setRoleName("TEACHER");
        else newUser.setRoleName("STUDENT");

        newUser.setAccountStatus("ACTIVE");
        newUser.setCreatedAt(new java.util.Date());

        // Lưu xuống Database thông qua UserRepository hiện tại của bạn
        return userRepository.save(newUser);
    }
}