package backend.repository;

import backend.entity.AcademicQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AcademicQuestionRepository extends JpaRepository<AcademicQuestion, Integer> {

    // Hàm tìm kiếm tất cả câu hỏi của một bài học, sắp xếp theo thời gian mới nhất lên đầu
    List<AcademicQuestion> findByLessonIdOrderByCreatedAtDesc(Integer lessonId);

    // Lấy tất cả câu hỏi kèm theo thông tin liên quan
    @org.springframework.data.jpa.repository.Query("SELECT q FROM AcademicQuestion q JOIN FETCH q.user JOIN FETCH q.lesson l JOIN FETCH l.chapter c JOIN FETCH c.course co ORDER BY q.createdAt DESC")
    List<AcademicQuestion> findAllQuestionsWithDetails();
}