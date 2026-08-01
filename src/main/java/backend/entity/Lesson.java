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

    // 🔥 ĐÃ SỬA: Thêm VARCHAR cho tiêu đề bài học
    @Column(name = "lesson_title", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String title;

    // 🔥 ĐÃ SỬA: Thêm LONGTEXT cho mô tả bài học
    @Column(name = "lesson_description", columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "is_preview", columnDefinition = "BIT DEFAULT 0")
    private Boolean isPreview = false;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "duration", length = 50)
    private String duration;

    @Column(name = "lesson_order")
    private Integer order;

    @JsonBackReference(value = "chapter-lessons")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private Chapter chapter;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<LearningMaterial> materials;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<InVideoQuestion> inVideoQuestions;

    // 🔥 ĐÃ SỬA: Thêm CascadeType.ALL để khi XÓA bài học sẽ tự động xóa hết các tiến độ, ghi chú, và hỏi đáp liên quan
    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<StudentProgress> studentProgresses;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<StudentNote> studentNotes;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<AcademicQuestion> academicQuestions;
}
