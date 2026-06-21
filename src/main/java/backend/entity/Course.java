package backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "Courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Integer id;

    @Column(name = "course_title", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String title;

    @Column(name = "course_description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "is_published")
    private Boolean isPublished;

    @Column(name = "teacher_id")
    private Integer teacherId;

    @Column(name = "subject_id")
    private Integer subjectId;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("order ASC")
    private List<Chapter> chapters;
}
