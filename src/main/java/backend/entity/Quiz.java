package backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "Quizzes")
@Getter @Setter @NoArgsConstructor
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "quiz_id")
    private Integer quizId;

    @Column(name = "course_id")
    private Integer courseId;

    @Column(name = "quiz_title")
    private String quizTitle;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    /** Loại quiz: ENTRY_TEST | PRACTICE | MOCK_EXAM */
    @Column(name = "quiz_type", length = 50)
    private String quizType;

    @OneToMany(mappedBy = "quiz", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Question> questions = new ArrayList<>();
}
