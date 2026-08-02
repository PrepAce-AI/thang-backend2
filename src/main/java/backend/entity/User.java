package backend.entity;
import lombok.*;
import jakarta.persistence.*;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Users")
@JsonIgnoreProperties({
        "hibernateLazyInitializer",
        "handler"
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private int id;

    // 🔥 ĐÃ SỬA: Thêm VARCHAR để lưu họ tên tiếng Việt đầy đủ dấu
    @Column(name = "full_name", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String fullName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "phone")
    private String phone;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "role_id", nullable = false)
    private int roleId;

    // 🔥 ĐÃ SỬA: Thêm VARCHAR để lưu trạng thái (Ví dụ: "Hoạt động", "Bị khóa")
    @Column(name = "account_status", columnDefinition = "VARCHAR(50)")
    private String accountStatus;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "verification_expiry")
    private Date verificationExpiry;

    // 🔥 ĐÃ SỬA: Thêm VARCHAR để lưu tên trường học tiếng Việt
    @Column(name = "school", columnDefinition = "NVARCHAR(255)")
    private String school;

    // 🔥 ĐÃ SỬA: Thêm LONGTEXT để lưu tiểu sử/giới thiệu bản thân
    @Column(name = "bio", columnDefinition = "LONGTEXT")
    private String bio;

    @Column(name = "role_name")
    private String roleName;

    @Column(name = "teacher_request_status")
    private String teacherRequestStatus; // Giá trị: NULL (Bình thường), "PENDING" (Đang chờ duyệt), "APPROVED", "REJECTED"

    @Column(name = "education")
    private String education;

    @Column(name = "experience")
    private String experience;
}
