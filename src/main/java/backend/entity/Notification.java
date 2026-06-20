package backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "Notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Integer notificationId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", columnDefinition = "NVARCHAR(MAX)")
    private String content;

    @Column(name = "target_role", nullable = false)
    private String targetRole;   // ALL, STUDENT, TEACHER

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "created_by")
    private Integer createdBy;

    // Relationship với User (người tạo thông báo)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    private User creator;
}
