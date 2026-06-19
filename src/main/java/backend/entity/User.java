package backend.entity;

import jakarta.persistence.*;
import java.util.Date;

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

    private String verificationCode;

    private Date verificationExpiry;

    // 🔥 ĐÃ SỬA: Thêm NVARCHAR để lưu tên trường học tiếng Việt
    @Column(name = "school", columnDefinition = "NVARCHAR(255)")
    private String school;

    // 🔥 ĐÃ SỬA: Thêm NVARCHAR(MAX) để lưu tiểu sử/giới thiệu bản thân
    @Column(name = "bio", columnDefinition = "NVARCHAR(MAX)")
    private String bio;

//    private boolean verified = false;

    public User() {}

    // getters & setters (Giữ nguyên không thay đổi)

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public int getRoleId() { return roleId; }
    public void setRoleId(int roleId) { this.roleId = roleId; }
    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
    public Date getVerificationExpiry() { return verificationExpiry; }
    public void setVerificationExpiry(Date verificationExpiry) { this.verificationExpiry = verificationExpiry; }
    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}