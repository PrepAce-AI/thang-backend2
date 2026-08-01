package backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Data
@Entity
@Table(name = "CourseReviews")
public class CourseReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Integer id;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    // Giữ nguyên LONGTEXT để lưu đánh giá tiếng Việt
    @Column(name = "comment", columnDefinition = "LONGTEXT")
    private String comment;

    /** Cột student_id (NOT NULL trong DB) — luôn gán = user.id khi tạo review */
    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
