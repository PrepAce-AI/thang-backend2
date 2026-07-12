package backend.service;
import backend.dto.response.CourseSummaryDTO;
import backend.dto.response.TeacherDashboardDTO;
import backend.entity.Course;
import backend.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherService {
    private final CourseRepository courseRepository;

    public TeacherDashboardDTO getDashboard(Integer teacherId) {

        List<Course> courses = courseRepository.findByTeacherId(teacherId);

        List<CourseSummaryDTO> courseDTOs = courses.stream().map(c -> {

            CourseSummaryDTO dto = new CourseSummaryDTO();

            dto.setCourseId(c.getCourseId());
            dto.setTitle(c.getTitle());
            dto.setStatus(c.getStatus());
            dto.setPublished(c.getIsPublished());

            dto.setChapterCount(
                    c.getChapters() != null ? c.getChapters().size() : 0
            );

            // placeholder nếu chưa có system student tracking
            dto.setStudentCount(0);
            dto.setRevenue(BigDecimal.ZERO);

            return dto;
        }).toList();

        TeacherDashboardDTO res = new TeacherDashboardDTO();
        res.setCourses(courseDTOs);
        res.setTotalCourses(courseDTOs.size());
        res.setTotalStudents(0);
        res.setTotalRevenue(BigDecimal.ZERO);

        return res;
    }
}
