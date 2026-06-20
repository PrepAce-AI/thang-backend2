package backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "QuestionOptions")
public class QuestionOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private int optionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    @JsonIgnore
    private Question question;

    @Column(name = "option_content", nullable = false)
    private String optionContent;

    public QuestionOption() {}

    public int getOptionId() { return optionId; }
    public void setOptionId(int optionId) { this.optionId = optionId; }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public String getOptionContent() { return optionContent; }
    public void setOptionContent(String optionContent) { this.optionContent = optionContent; }
}
