package backend.service;

import backend.entity.Notification;
import backend.entity.Course;
import backend.entity.User;
import backend.repository.CourseRepository;
import backend.repository.UserRepository;
import backend.repository.NotificationRepository;

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

    public AdminService(CourseRepository courseRepository, UserRepository userRepository, NotificationRepository notificationRepository) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    // ==================== COURSES ====================
    public List<Course> getAllCourse(){
        return courseRepository.findAll();
    }

    @Transactional
    public Course updateCourseStatus(Integer courseId, String status, String note){
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học"));
        course.setStatus(status);
        course.setReviewNote(note);

        if ("PUBLISHED".equals(status)){
            course.setIsPublished(true);
        }else if ("REJECTED".equals(status)){
            course.setIsPublished(false);
        }

        return courseRepository.save(course);
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

    // ==================== STATISTICS ====================
    public Map<String, Long> getDashboardStats(){
        long totalUsers = userRepository.count();
        long totalCourses = courseRepository.count();
        long publishedCourses = courseRepository.findAll().stream().filter(c -> "PUBLISHED".equals(c.getStatus())).count();

        return Map.of("totalUsers", totalUsers,
                        "totalCourses", totalCourses,
                        "publishedCourses", publishedCourses);
    }

    // ==================== NOTIFICATIONS ====================
    public List<Notification> getAllNotifications() {
        return notificationRepository.findTop10ByOrderByCreatedAtDesc();
    }
}
