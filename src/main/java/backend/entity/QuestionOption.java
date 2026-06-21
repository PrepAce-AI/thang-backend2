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
