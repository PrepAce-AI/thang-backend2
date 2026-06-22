package backend.dto.response;

import java.math.BigDecimal;

public class CourseSummaryDTO {
    private Integer courseId;
    private String title;
    private String status;
    private Boolean isPublished;
    private Integer chapterCount;

    private Integer studentCount;   // cần query riêng
    private BigDecimal revenue;     // nếu có payment

    public CourseSummaryDTO() {
    }

    public CourseSummaryDTO(Integer courseId, String title, String status, Boolean isPublished, Integer chapterCount, Integer studentCount, BigDecimal revenue) {
        this.courseId = courseId;
        this.title = title;
        this.status = status;
        this.isPublished = isPublished;
        this.chapterCount = chapterCount;
        this.studentCount = studentCount;
        this.revenue = revenue;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getPublished() {
        return isPublished;
    }

    public void setPublished(Boolean published) {
        isPublished = published;
    }

    public Integer getChapterCount() {
        return chapterCount;
    }

    public void setChapterCount(Integer chapterCount) {
        this.chapterCount = chapterCount;
    }

    public Integer getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(Integer studentCount) {
        this.studentCount = studentCount;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }
}
