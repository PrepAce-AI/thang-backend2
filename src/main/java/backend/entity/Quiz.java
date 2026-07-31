package backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.*;

@Entity
@Table(name = "Quizzes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_id")
    private Integer quizId;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
// Không dùng @JsonIgnore ở đây để Frontend đọc được object course
    private Course course;

    @OneToMany(
            mappedBy = "quiz",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JsonManagedReference // Quét dữ liệu xuôi xuống Question khi xem chi tiết đề
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<Question> questions = new ArrayList<>();

    @Column(name = "quiz_title", columnDefinition = "NVARCHAR(255)")
    private String quizTitle;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    /** Loại quiz: ENTRY_TEST | PRACTICE | MOCK_EXAM */
    @Column(name = "quiz_type", length = 50)
    private String quizType;

    @Column(name = "subject", length = 50)
    private String subject;

    @Column(name = "is_entry_test")
    private Boolean isEntryTest = false;
}
