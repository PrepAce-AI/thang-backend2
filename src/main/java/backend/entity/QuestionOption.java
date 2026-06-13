package backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "QuestionOptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "option_id")
    private int optionId;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "option_content", nullable = false)
    private String optionContent;
}
