package backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO trả về thông tin giảng viên cho trang InstructorProfilePage (học sinh xem).
 * Endpoint: GET /api/users/instructor/{id}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstructorProfileResponse {

    private InstructorInfo info;
    private List<InstructorCourse> courses;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstructorInfo {
        private int id;
        private String name;
        private String avatar;
        private String bio;
        private String school;
        private String subject;       // Môn học chính (suy ra từ khóa học)
        private InstructorStats stats;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstructorStats {
        private long students;   // Tổng học viên (đếm từ Enrollments/Payments)
        private int courses;     // Tổng số khóa học
        private double rating;   // Rating trung bình
        private long reviews;    // Tổng số đánh giá
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstructorCourse {
        private int id;
        private String title;
        private String price;
        private long students;
        private String thumbnail;
    }
}
