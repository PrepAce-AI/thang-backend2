package backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data
@Entity
@Table(name = "Lessons")
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lesson_id")
    private Integer id;

    // 🔥 ĐÃ SỬA: Thêm NVARCHAR cho tiêu đề bài học
    @Column(name = "lesson_title", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String title;

    // 🔥 ĐÃ SỬA: Thêm NVARCHAR(MAX) cho mô tả bài học
    @Column(name = "lesson_description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "duration")
    private String duration;

    @Column(name = "lesson_order")
    private Integer order;

    @JsonBackReference(value = "chapter-lessons")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<LearningMaterial> materials;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InVideoQuestion> inVideoQuestions;
}