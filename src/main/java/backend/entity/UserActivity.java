package backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "User_Activities")
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "action", nullable = false)
    private String action; // Hành động (Ví dụ: "Đăng nhập", "Làm bài thi thử...")

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    // Chạy lệnh SQL này trong SSMS để tạo bảng thật:
    /*
    CREATE TABLE User_Activities (
        id INT IDENTITY(1,1) PRIMARY KEY,
        user_id INT,
        action LONGTEXT NOT NULL,
        timestamp DATETIME DEFAULT GETDATE()
    );
    */

    // Boilerplate Code: Getter, Setter, Constructors
    public UserActivity() {}

    public UserActivity(Integer userId, String action, LocalDateTime timestamp) {
        this.userId = userId;
        this.action = action;
        this.timestamp = timestamp;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
