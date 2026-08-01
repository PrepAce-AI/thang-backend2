package backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "target_role", nullable = false)
    private String targetRole;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "created_by")
    private Integer createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    @JsonIgnore
    private User creator;

    // =========================================================================
    // 🔥 ĐÃ SỬA: Ép quyền ghi nhận giá trị (insertable/updatable) cho biến số.
    // Chống tình trạng Hibernate ưu tiên Object liên kết khiến cột bị dính NULL.
    // =========================================================================
    @Column(name = "user_id", insertable = true, updatable = true)
    @JsonProperty("user_id")
    private Integer receiverId;

    // =========================================================================
    // 💡 PHÒNG NGỪA: Nếu trong file Entity cũ từng có khai báo Object liên kết
    // đến User nhận thông báo (đè lên cột user_id), hãy đảm bảo nó được chặn ghi đè:
    // =========================================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false) // Chặn object này tranh quyền lưu với receiverId
    @JsonIgnore
    private User receiverUser;
}
