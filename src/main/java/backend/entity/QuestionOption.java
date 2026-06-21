package backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "QuestionOptions")
@Getter @Setter @NoArgsConstructor
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private Integer optionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "option_content")
    private String optionContent;

    public QuestionOption() {}

    public int getOptionId() { return optionId; }
    public void setOptionId(int optionId) { this.optionId = optionId; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public String getOptionContent() { return optionContent; }
    public void setOptionContent(String optionContent) { this.optionContent = optionContent; }
}
