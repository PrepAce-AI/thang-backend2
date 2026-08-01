package backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "Courses")
@JsonIgnoreProperties({
        "hibernateLazyInitializer",
        "handler"
})
@Getter
@Setter
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Integer courseId;

    @Column(name = "course_title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "teacher_id")
    private Integer teacherId;

    @Column(name = "subject_id")
    private Integer subjectId;

    @Column(name = "status")
    private String status = "DRAFT";

    @Column(name = "note")
    private String reviewNote;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt = new Date();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", insertable = false, updatable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "teacher_id", insertable = false, updatable = false)
    private backend.entity.User teacher;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "course_id")
    @org.hibernate.annotations.BatchSize(size = 50)
    @JsonIgnore
    private List<backend.entity.Chapter> chapters;

    @Column(name = "category_id")
    private Integer categoryId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    
    private Category category;

    // Thực thể Category quản lý tên danh mục
    // ==================== GETTERS AND SETTERS ====================

    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }

    public boolean getIsPublished() {
        return "PUBLISHED".equalsIgnoreCase(this.status);
    }

    public void setIsPublished(boolean isPublished) {
        this.status = isPublished ? "PUBLISHED" : "DRAFT";
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getTeacherId() { return teacherId; }
    public void setTeacherId(Integer teacherId) { this.teacherId = teacherId; }

    public Integer getSubjectId() { return subjectId; }
    public void setSubjectId(Integer subjectId) { this.subjectId = subjectId; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public List<backend.entity.Chapter> getChapters() { return chapters; }
    public void setChapters(List<backend.entity.Chapter> chapters) { this.chapters = chapters; }


    public backend.entity.User getTeacher() { return teacher; }
    public void setTeacher(backend.entity.User teacher) { this.teacher = teacher; }

    // --- Thêm Getter và Setter cho thuộc tính mới ---
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

}
