package backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data
@Entity
@Table(name = "Courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Integer id;

    // 🔥 ĐÃ SỬA: Thêm NVARCHAR cho tiêu đề khóa học
    @Column(name = "course_title", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String title;

    // 🔥 ĐÃ SỬA: Thêm NVARCHAR(MAX) cho mô tả khóa học dài
    @Column(name = "course_description", columnDefinition = "NVARCHAR(MAX)")
    private String description;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "Courses")
@Getter @Setter @NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Integer courseId;

    @Column(name = "teacher_id", nullable = false)
    private Integer teacherId;

    @Column(name = "subject_id", nullable = false)
    private Integer subjectId;

    @Column(name = "course_title")
    private String courseTitle;

    @Column(name = "course_description", columnDefinition = "NVARCHAR(MAX)")
    private String courseDescription;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("order ASC")
    private List<Chapter> chapters;


    // 🔥 BẢO TOÀN THÊM ĐOẠN NÀY VÀO CUỐI FILE ĐỂ ĐỒNG BỘ VỚI SQL SERVER:
    @Column(name = "price")
    private java.math.BigDecimal price;
    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "is_published")
    private Boolean isPublished;

    @Column(name = "teacher_id")
    private Integer teacherId;

    @Column(name = "subject_id")
    private Integer subjectId;
}
    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
}
