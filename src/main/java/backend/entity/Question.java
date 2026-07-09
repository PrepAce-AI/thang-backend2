package backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "questions")
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

    @Column(name = "content", columnDefinition = "NVARCHAR(MAX)", nullable = false)
    private String questionContent;

    @Column(name = "explanation", columnDefinition = "NVARCHAR(MAX)")
    private String explanation;

    @Column(name = "topic")
    private String topic;

    @Column(name = "subject")
    private String subject;

    @Column(name = "created_at", insertable = false, updatable = true)
    public Date createdAt;

    @Column(name = "difficulty")
    private Integer difficulty;

    @OneToMany(mappedBy = "question",
            fetch = FetchType.EAGER,
            cascade = CascadeType.ALL)
    private List<QuestionOption> options = new ArrayList<>();

    @OneToMany(mappedBy = "question")
    private List<StudentAnswer> studentAnswers = new ArrayList<>();
    @Column(name = "question_type", length = 50)
    private String questionType;
}
