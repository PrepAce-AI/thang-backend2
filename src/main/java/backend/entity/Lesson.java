package backend.entity;

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

    @Column(name = "lesson_title", nullable = false)
    private String title;

    @Column(name = "lesson_description")
    private String description;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "duration")
    private String duration;

    @Column(name = "lesson_order")
    private Integer order;

    // Trỏ về Chương
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    // Thêm vào trong class Lesson
    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<LearningMaterial> materials;
}