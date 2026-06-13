package backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private int courseId;

    @Column(name = "teacher_id", nullable = false)
    private Integer teacherId;

    @Column(name = "subject_id", nullable = false)
    private Integer subjectId;

    @Column(name = "course_title", nullable = false)
    private String title;

    @Column(name = "course_description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "price")
    private Double price;

    @Column(name = "is_published")
    private Boolean isPublished;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

//    public Course() {
//    }
//
//    public Course(int id, Integer teacherId, Integer subjectId, String title, String description, String thumbnailUrl, Double price, Boolean isPublished, LocalDateTime createdAt) {
//        this.id = id;
//        this.teacherId = teacherId;
//        this.subjectId = subjectId;
//        this.title = title;
//        this.description = description;
//        this.thumbnailUrl = thumbnailUrl;
//        this.price = price;
//        this.isPublished = isPublished;
//        this.createdAt = createdAt;
//    }


    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public Integer getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
    }

    public Integer getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Integer subjectId) {
        this.subjectId = subjectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Boolean getPublished() {
        return isPublished;
    }

    public void setPublished(Boolean published) {
        isPublished = published;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
