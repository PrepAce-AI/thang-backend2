package backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Entity
@Table(name = "ViolationReports") // Thay tên bảng đúng theo DB Script của bạn nếu khác
@Data
public class ViolationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId; // Hoặc reporterId tùy thuộc database

    @Column(name = "reported_target")
    private String reportedTarget;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "status")
    private String status; // PENDING, RESOLVED_BAN, DISMISSED

    @Column(name = "created_at")
    private Date createdAt = new Date();
}