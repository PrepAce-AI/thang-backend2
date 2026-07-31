package backend.dto.response;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonProperty; // 🔥 THÊM IMPORT NÀY

public class CourseListResponse {
    private Integer id;
    private String title;
    private String description;

    @JsonProperty("thumbnail_url") // 🔥 THÊM DÒNG NÀY: Ép Spring Boot nhả JSON key là thumbnail_url
    private String thumbnailUrl;

    private BigDecimal price;
    private Boolean isPublished;
    private Integer students = 0;

    private Integer subjectId;
    private String subjectName;
    private Integer teacherId;
    private String teacherName;

    // Toàn bộ các hàm Constructor, Getter, Setter bên dưới của Toàn GIỮ NGUYÊN VẸN 100%
    public CourseListResponse() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Boolean getIsPublished() { return isPublished; }
    public void setIsPublished(Boolean isPublished) { this.isPublished = isPublished; }

    public Integer getStudents() {
        return students;
    }

    public void setStudents(Integer students) {
        this.students = students;
    }

    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public Integer getTeacherId() { return teacherId; }
    public void setTeacherId(Integer teacherId) { this.teacherId = teacherId; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
}