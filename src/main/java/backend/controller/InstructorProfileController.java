package backend.controller;

import backend.dto.response.InstructorProfileResponse;
import backend.entity.Course;
import backend.entity.CourseReview;
import backend.entity.User;
import backend.repository.CourseRepository;
import backend.repository.CourseReviewRepository;
import backend.repository.EnrollmentRepository;
import backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller công khai cho trang hồ sơ giảng viên.
 * Học sinh bấm vào tên giáo viên ở HomePage / CoursesPage / CourseDetailPage
 * sẽ gọi GET /api/users/instructor/{id} để xem thông tin giảng viên.
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173")
public class InstructorProfileController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseReviewRepository courseReviewRepository;

    public InstructorProfileController(UserRepository userRepository,
                                       CourseRepository courseRepository,
                                       EnrollmentRepository enrollmentRepository,
                                       CourseReviewRepository courseReviewRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseReviewRepository = courseReviewRepository;
    }

    @GetMapping("/instructor/{id}")
    public ResponseEntity<?> getInstructorProfile(@PathVariable Integer id) {
        // 1. Lấy thông tin user (giáo viên)
        Optional<User> optUser = userRepository.findById(id);
        if (optUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User teacher = optUser.get();

        // Kiểm tra role là TEACHER
        if (teacher.getRoleId() != 2 && !"TEACHER".equalsIgnoreCase(teacher.getRoleName())) {
            return ResponseEntity.badRequest().body(Map.of("message", "User này không phải giảng viên."));
        }

        // 2. Lấy danh sách khóa học của giáo viên
        List<Course> teacherCourses = courseRepository.findByTeacherId(id);

        // 3. Tính tổng số học viên từ Enrollments
        long totalStudents = 0;
        for (Course course : teacherCourses) {
            totalStudents += enrollmentRepository.countByCourseId(course.getCourseId());
        }

        // 4. Tính rating trung bình và tổng số đánh giá từ CourseReviews
        double totalRating = 0;
        long totalReviews = 0;
        for (Course course : teacherCourses) {
            List<CourseReview> reviews = courseReviewRepository
                    .findByCourse_CourseIdOrderByCreatedAtDesc(course.getCourseId());
            for (CourseReview review : reviews) {
                totalRating += review.getRating();
                totalReviews++;
            }
        }
        double avgRating = totalReviews > 0 ? Math.round((totalRating / totalReviews) * 10.0) / 10.0 : 0;

        // 5. Suy ra môn học chính từ khóa học (lấy môn xuất hiện nhiều nhất)
        String mainSubject = inferMainSubject(teacherCourses);

        // 6. Xây dựng avatar URL
        String avatarUrl = teacher.getAvatarUrl();
        if (avatarUrl == null || avatarUrl.isBlank() || "null".equals(avatarUrl)) {
            avatarUrl = "https://ui-avatars.com/api/?name=" + encodeURIComponent(teacher.getFullName()) + "&background=64748b&color=fff";
        } else if (!avatarUrl.startsWith("http")) {
            avatarUrl = "http://localhost:8080" + avatarUrl;
        }

        // 7. Build response
        InstructorProfileResponse.InstructorStats stats = new InstructorProfileResponse.InstructorStats(
                totalStudents,
                teacherCourses.size(),
                avgRating,
                totalReviews
        );

        InstructorProfileResponse.InstructorInfo info = new InstructorProfileResponse.InstructorInfo(
                teacher.getId(),
                teacher.getFullName(),
                avatarUrl,
                teacher.getBio() != null ? teacher.getBio() : "",
                teacher.getSchool() != null ? teacher.getSchool() : "",
                mainSubject,
                stats
        );

        // 8. Build danh sách khóa học
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        List<InstructorProfileResponse.InstructorCourse> courseList = teacherCourses.stream()
                .map(c -> {
                    long courseStudents = enrollmentRepository.countByCourseId(c.getCourseId());

                    String priceStr = "Miễn phí";
                    if (c.getPrice() != null && c.getPrice().doubleValue() > 0) {
                        priceStr = formatter.format(c.getPrice().longValue()) + "đ";
                    }

                    String thumb = c.getThumbnailUrl();
                    if (thumb != null && !thumb.startsWith("http")) {
                        thumb = "http://localhost:8080" + thumb;
                    }

                    return new InstructorProfileResponse.InstructorCourse(
                            c.getCourseId(),
                            c.getTitle(),
                            priceStr,
                            courseStudents,
                            thumb
                    );
                })
                .collect(Collectors.toList());

        InstructorProfileResponse response = new InstructorProfileResponse(info, courseList);
        return ResponseEntity.ok(response);
    }

    /**
     * Suy ra môn học chính dựa trên khóa học của giáo viên.
     */
    private String inferMainSubject(List<Course> courses) {
        if (courses.isEmpty()) return "Giảng viên";

        Map<String, Long> subjectCount = new HashMap<>();
        for (Course c : courses) {
            if (c.getSubject() != null && c.getSubject().getSubjectName() != null) {
                String name = translateSubject(c.getSubject().getSubjectName());
                subjectCount.merge(name, 1L, Long::sum);
            }
        }

        if (subjectCount.isEmpty()) return "Giảng viên";

        return subjectCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Giảng viên");
    }

    private String translateSubject(String englishName) {
        if (englishName == null) return "Khác";
        Map<String, String> map = Map.of(
                "Mathematics", "Toán Học",
                "Physics", "Vật Lý",
                "Chemistry", "Hóa Học",
                "Literature", "Ngữ Văn",
                "English", "Tiếng Anh",
                "History", "Lịch Sử",
                "Geography", "Địa Lý"
        );
        return map.getOrDefault(englishName, englishName);
    }

    private String encodeURIComponent(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }
}
