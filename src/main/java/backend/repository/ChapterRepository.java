package backend.repository;

import backend.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Integer> {
    // Tìm kiếm tất cả các chương thuộc về một khóa học và sắp xếp theo thứ tự (chapter_order)
    List<Chapter> findByCourse_CourseIdOrderByOrderAsc(Integer courseId);
}