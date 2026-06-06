package backend.repository;

import backend.entity.AcademicQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AcademicQuestionRepository extends JpaRepository<AcademicQuestion, Integer> {

    // Hàm tìm kiếm tất cả câu hỏi của một bài học, sắp xếp theo thời gian mới nhất lên đầu
    List<AcademicQuestion> findByLessonIdOrderByCreatedAtDesc(Integer lessonId);
}