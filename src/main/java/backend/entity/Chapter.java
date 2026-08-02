package backend.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data
@Entity
@Table(name = "Chapters")
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chapter_id")
    private Integer id;

    // 🔥 ĐÃ SỬA: Thêm VARCHAR để lưu tiêu đề chương tiếng Việt
    @Column(name = "chapter_title", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String title;

    @Column(name = "chapter_order")
    private Integer order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    @com.fasterxml.jackson.annotation.JsonBackReference(value = "course-chapters") // 🔥 THÊM DÒNG NÀY
    private Course course;

    @JsonManagedReference(value = "chapter-lessons")
    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("order ASC")
    @org.hibernate.annotations.BatchSize(size = 50)
    private List<Lesson> lessons;
}
