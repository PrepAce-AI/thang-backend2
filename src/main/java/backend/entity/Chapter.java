package backend.entity;

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

    @Column(name = "chapter_title", nullable = false)
    private String title;

    @Column(name = "chapter_order")
    private Integer order;

    // Trỏ về Khóa học
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    // Quan hệ 1 Chương có nhiều Bài giảng
    @OneToMany(mappedBy = "chapter", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @OrderBy("order ASC") // Sắp xếp lesson theo thứ tự
    private List<Lesson> lessons;
}