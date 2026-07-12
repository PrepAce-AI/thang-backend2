package backend.entity;
import lombok.*;
import jakarta.persistence.*;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private int id;

    // 🔥 ĐÃ SỬA: Thêm NVARCHAR để lưu họ tên tiếng Việt đầy đủ dấu
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

    // 🔥 ĐÃ SỬA: Thêm NVARCHAR để lưu trạng thái (Ví dụ: "Hoạt động", "Bị khóa")
    @Column(name = "account_status", columnDefinition = "NVARCHAR(50)")
    private String accountStatus;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "verification_expiry")
    private Date verificationExpiry;

    // 🔥 ĐÃ SỬA: Thêm NVARCHAR để lưu tên trường học tiếng Việt
    @Column(name = "school", columnDefinition = "NVARCHAR(255)")
    private String school;

    // 🔥 ĐÃ SỬA: Thêm NVARCHAR(MAX) để lưu tiểu sử/giới thiệu bản thân
    @Column(name = "bio", columnDefinition = "NVARCHAR(MAX)")
    private String bio;

    @Column(name = "role_name")
    private String roleName;

    @Column(name = "failed_attempts")
    private int failedAttempts;

    @Column(name = "lockout_expiry")
    private Date lockoutExpiry;

    @Column(name = "otp_resend_count")
    private int otpResendCount;

    @Column(name = "otp_failed_attempts")
    private int otpFailedAttempts;

    @Column(name = "change_pw_failed_attempts")
    private int changePwFailedAttempts;

    @Column(name = "change_pw_lockout_expiry")
    private Date changePwLockoutExpiry;

    @Column(name = "token_version")
    private int tokenVersion = 1;

    @Column(name = "reset_token")
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private Date resetTokenExpiry;
}