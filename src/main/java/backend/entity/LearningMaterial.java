package backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@Entity
@Table(name = "LearningMaterials") // 🔥 Giữ nguyên viết liền hoa thường
public class LearningMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_id") // Khớp hoàn toàn với DB
    private Integer id;

    @Column(name = "material_title", nullable = false) // Khớp hoàn toàn với DB
    private String title;

    @Column(name = "file_url", nullable = false) // Khớp hoàn toàn với DB
    private String fileUrl;

    @Column(name = "uploaded_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date uploadedAt = new Date();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lesson_id", referencedColumnName = "lesson_id", nullable = false)
    private Lesson lesson;
}