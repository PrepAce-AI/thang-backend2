package backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;

@Data // Tự động sinh Get/Set với Lombok
@Entity
@Table(name = "Courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_id")
    private Integer id;

    @Column(name = "course_title", nullable = false)
    private String title;

    @Column(name = "course_description")
    private String description;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    // Quan hệ 1 Khóa học có nhiều Chương
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("order ASC") // Sắp xếp chapter theo thứ tự
    private List<Chapter> chapters;
}