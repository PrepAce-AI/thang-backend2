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
    @Column(name = "answer_id")
    private int answerId;

    @ManyToOne
    @JoinColumn(name = "sessions_id", nullable = false)
    @JsonIgnore
    private TestSession session;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore //Tranh lap quiz
    private Question question;

    @ManyToOne
    @JoinColumn(name = "selected_option_id")
    @JsonIgnore
    private QuestionOption selectedOption;

    @Column(name = "answered_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date answeredAt;
}
