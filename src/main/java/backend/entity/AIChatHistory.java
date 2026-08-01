package backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "AIChatHistory")
@Getter @Setter @NoArgsConstructor
public class AIChatHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_id")
    private Integer chatId;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "question", columnDefinition = "LONGTEXT")
    private String question;

    @Column(name = "ai_response", columnDefinition = "LONGTEXT")
    private String aiResponse;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    /** Loại request: CHAT | GAP_DIAGNOSIS | SCORE_FORECAST | UNIVERSITY_ADVISE | POST_LESSON_QUIZ */
    @Column(name = "request_type", length = 50)
    private String requestType;
}
