package backend.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Entity
@Table(name = "test_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TestSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sessions_id")
    private int sessionsId;

    @ManyToOne
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User student;

    @Column(name = "started_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startedAt;

    @Column(name = "submitted_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date submittedAt;

    @Column(name = "remaining_time")
    private int remainingTime;

    @Column(name = "status")
    private String status = "IN_PROGRESS";

    @Column(name = "score")
    private Float score;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;
}
