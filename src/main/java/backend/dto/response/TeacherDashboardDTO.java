package backend.dto.response;
import backend.entity.Course;

import java.math.BigDecimal;
import java.util.List;

public class TeacherDashboardDTO {
    private List<CourseSummaryDTO> courses;
    private Integer totalCourses;
    private Integer totalStudents;
    private BigDecimal totalRevenue;

    public TeacherDashboardDTO() {
    }

    public TeacherDashboardDTO(List<CourseSummaryDTO> courses, Integer totalCourses, Integer totalStudents, BigDecimal totalRevenue) {
        this.courses = courses;
        this.totalCourses = totalCourses;
        this.totalStudents = totalStudents;
        this.totalRevenue = totalRevenue;
    }

    public List<CourseSummaryDTO> getCourses() {
        return courses;
    }

    public void setCourses(List<CourseSummaryDTO> courses) {
        this.courses = courses;
    }

    public Integer getTotalCourses() {
        return totalCourses;
    }

    public void setTotalCourses(Integer totalCourses) {
        this.totalCourses = totalCourses;
    }

    public Integer getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(Integer totalStudents) {
        this.totalStudents = totalStudents;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }
}
