package backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "QuestionOptions")
@Getter @Setter
public class QuestionOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private Integer optionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore
    private Question question;

    @Column(name = "option_content", columnDefinition = "LONGTEXT", nullable = false)
    private String optionContent;

    @Column(name = "is_correct", nullable = false)
    private Boolean isCorrect = false;

    public QuestionOption() {}
}
