package backend.service;

import backend.dto.response.CourseListResponse;
import backend.entity.Course;
import backend.entity.Enrollment;
import backend.repository.CourseRepository;
import backend.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public List<CourseListResponse> getMyEnrolledCourses(Integer studentId) {
        // 1. Tìm toàn bộ các bản ghi đăng ký học của học sinh này
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);

        // 2. Gom danh sách ID các khóa học đã mua
        List<Integer> courseIds = enrollments.stream()
                .map(Enrollment::getCourseId)
                .collect(Collectors.toList());

        if (courseIds.isEmpty()) {
            return new ArrayList<>(); // Trả về danh sách rỗng nếu chưa mua khóa nào
        }

        // 3. Lấy toàn bộ thông tin chi tiết của các khóa học đó từ DB
        List<Course> courses = courseRepository.findAllById(courseIds);

        // 4. Map toàn bộ sang DTO chuẩn để gửi cho React (Đã đồng bộ thumbnail_url và teacherName)
        return courses.stream().map(course -> {
            CourseListResponse dto = new CourseListResponse();
            dto.setId(course.getCourseId());
            dto.setTitle(course.getTitle());
            dto.setDescription(course.getDescription());
            dto.setThumbnailUrl(course.getThumbnailUrl()); // Sẽ tự biến thành thumbnail_url nhờ @JsonProperty
            dto.setPrice(course.getPrice());
            dto.setIsPublished(course.getIsPublished());

            // Đồng bộ môn học
            if (course.getSubject() != null) {
                dto.setSubjectId(course.getSubject().getId());
                dto.setSubjectName(course.getSubject().getSubjectName());
            } else {
                dto.setSubjectId(course.getSubjectId());
                dto.setSubjectName("Chung");
            }

            // Đồng bộ giáo viên
            if (course.getTeacher() != null) {
                dto.setTeacherId(course.getTeacher().getId());
                dto.setTeacherName(course.getTeacher().getFullName());
            } else {
                dto.setTeacherId(course.getTeacherId());
                dto.setTeacherName("Giáo viên");
            }

            return dto;
        }).collect(Collectors.toList());
    }
}