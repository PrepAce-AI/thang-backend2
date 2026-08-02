package backend.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
@Entity
@Table(name = "StudentAnswers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int answerId;

    @ManyToOne
    @JoinColumn(name = "attempt_id", nullable = false)
    @JsonIgnore
    private TestSession session;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore //Tranh lap quiz
    private Question question;

    @ManyToOne
    @JoinColumn(name = "selected_option_id", nullable = true)
    @JsonIgnore
    private QuestionOption selectedOption;

    @Column(name = "essay_answer", columnDefinition = "LONGTEXT")
    private String essayAnswer; // Lưu bài làm tự luận của học sinh

    @Column(name = "score")
    private Float score; // Lưu điểm riêng của câu này (phục vụ chấm câu tự luận)

    @Column(name = "teacher_comment", columnDefinition = "NVARCHAR(MAX)")
    private String teacherComment;

    @Column(name = "answered_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date answeredAt;
}
