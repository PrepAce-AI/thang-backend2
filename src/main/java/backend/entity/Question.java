package backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Integer questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    @JsonIgnore
    private Quiz quiz;

    @Column(name = "question_content", columnDefinition = "NVARCHAR(MAX)")
    private String questionContent;

    @Column(name = "correct_answer")
    private String correctAnswer;

    @Column(name = "explanation")
    private String explanation;
    /** Mức độ nhận thức: 1-Nhận biết, 2-Thông hiểu, 3-Vận dụng, 4-Vận dụng cao */
    @Column(name = "cognitive_level")
    private Integer cognitiveLevel;

    @OneToMany(mappedBy = "question",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL)
    private List<QuestionOption> options = new ArrayList<>();

    @OneToMany(mappedBy = "question")
    private List<StudentAnswer> studentAnswers = new ArrayList<>();
    @Column(name = "question_type", length = 50)
    private String questionType;
}
