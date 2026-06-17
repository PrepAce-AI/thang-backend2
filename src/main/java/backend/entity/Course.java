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

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("order ASC")
    private List<Chapter> chapters;


    // 🔥 BẢO TOÀN THÊM ĐOẠN NÀY VÀO CUỐI FILE ĐỂ ĐỒNG BỘ VỚI SQL SERVER:
    @Column(name = "price")
    private java.math.BigDecimal price;

    @Column(name = "is_published")
    private Boolean isPublished;

    @Column(name = "teacher_id")
    private Integer teacherId;

    @Column(name = "subject_id")
    private Integer subjectId;
}