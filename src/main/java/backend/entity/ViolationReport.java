package backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;

@Entity
@Table(name = "ViolationReports")
@Data
public class ViolationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "reporter_id")
    private Integer reporterId; // Người gửi đơn tố cáo

    @Column(name = "reported_target", columnDefinition = "NVARCHAR(1000)")
    private String reportedTarget; // Link hoặc Tên đối tượng bị tố cáo (Ví dụ: Khóa học ID #4, Bình luận...)

    @Column(name = "reason", columnDefinition = "NVARCHAR(1000)")
    private String reason; // Lý do tố cáo do người dùng viết

    @Column(name = "status")
    private String status; // PENDING, RESOLVED_BAN, DISMISSED

    @Column(name = "admin_note", columnDefinition = "NVARCHAR(1000)")
    private String adminNote; // Ghi chú phản hồi của Admin khi xử lý xong

    @Column(name = "created_at")
    private Date createdAt = new Date();
}
