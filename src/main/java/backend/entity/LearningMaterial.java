package backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@Entity
@Table(name = "LearningMaterials")
public class LearningMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_id")
    private Integer id;

    // 🔥 ĐÃ SỬA: Thêm VARCHAR để lưu tiêu đề tài liệu tiếng Việt
    @Column(name = "material_title", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String title;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "uploaded_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date uploadedAt = new Date();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lesson_id", referencedColumnName = "lesson_id", nullable = false)
    private Lesson lesson;
}
