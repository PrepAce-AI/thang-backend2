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
    private int questionId;

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    @JsonIgnore
    private Quiz quiz;

    @Column(name = "question_content", nullable = false)
    private String questionContent;

    @Column(name = "correct_answer")
    private String correctAnswer;

    @Column(name = "explanation")
    private String explanation;

    @OneToMany(mappedBy = "question",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL)
    private List<QuestionOption> options = new ArrayList<>();

    @OneToMany(mappedBy = "question")
    private List<StudentAnswer> studentAnswers = new ArrayList<>();
}
