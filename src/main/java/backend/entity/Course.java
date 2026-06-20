package backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

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
    private BigDecimal price;

    @Column(name = "is_published")
    private Boolean isPublished;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // === FIELDS MỚI CHO ADMIN ===
    @Column(name = "status")
    private String status;           // PENDING, PUBLISHED, REJECTED

    @Column(name = "review_note", columnDefinition = "NVARCHAR(MAX)")
    private String reviewNote;

    @Column(name = "reviewed_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date reviewedAt;
}
