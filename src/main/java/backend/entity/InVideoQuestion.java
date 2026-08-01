package backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "in_video_questions")
public class InVideoQuestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "timestamp_seconds", nullable = false)
    private Integer timestampSeconds;

    @Column(name = "question_text", nullable = false, columnDefinition = "LONGTEXT")
    private String questionText;

    @Column(name = "option_a", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String optionA;

    @Column(name = "option_b", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String optionB;

    @Column(name = "option_c", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String optionC;

    @Column(name = "option_d", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String optionD;

    @Column(name = "correct_option", nullable = false, length = 1)
    private String correctOption; // 'A', 'B', 'C', 'D'

    @JsonBackReference(value = "lesson-questions")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;
}
