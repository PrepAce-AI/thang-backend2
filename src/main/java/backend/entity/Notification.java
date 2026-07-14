package backend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore; // 🔥 THÊM DÒNG IMPORT NÀY
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
    private String targetRole;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "created_by")
    private Integer createdBy;

    // =========================================================
    // 🔥 SỬA TẠI ĐÂY: Thêm @JsonIgnore để chặn đứng lỗi 500 JSON
    // =========================================================
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    @JsonIgnore // 👈 THÊM DÒNG NÀY VÀO
    private User creator;

    @Column(name = "user_id")
    @JsonProperty("user_id")
    private Integer receiverId;
}