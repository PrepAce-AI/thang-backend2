package backend.repository;

import backend.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Integer> {

    // 🔥 GOM DỮ LIỆU COURSE ĐI KÈM NGAY TỪ TẦNG DATABASE ĐỂ CHỐNG LỖI LAZY/JSON
    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.course")
    List<Quiz> findAllWithCourse();

    List<Quiz> findByCourse_CourseId(Integer courseId);

    @Query("""
SELECT q
FROM Quiz q
WHERE q.quizType = 'ENTRY_TEST'
AND (q.course.courseId = :courseId OR :courseId IS NULL)
""")
    Optional<Quiz> findEntryTestByCourseId(@Param("courseId") Integer courseId);

    @Query("SELECT q FROM Quiz q WHERE q.quizType = 'ENTRY_TEST'")
    List<Quiz> findAllEntryTests();

    @Query("""
SELECT q FROM Quiz q
WHERE q.quizType IN ('ENTRY_TEST', 'PRACTICE', 'MOCK_EXAM')
AND (:type IS NULL OR q.quizType = :type)
AND (:subject IS NULL OR q.subject = :subject)
ORDER BY q.quizType, q.subject
""")
    List<Quiz> findExamQuizzes(@Param("type") String type, @Param("subject") String subject);
    
    @Query("SELECT q FROM Quiz q LEFT JOIN FETCH q.questions WHERE q.quizId = :quizId")
    Optional<Quiz> findByIdWithQuestions(@Param("quizId") Integer quizId);
}