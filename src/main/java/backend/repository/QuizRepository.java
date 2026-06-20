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

    List<Quiz> findByCourseId(Integer courseId);

    /** Lấy Entry Test theo course_id (nếu có) hoặc standalone (course_id null) */
    @Query("SELECT q FROM Quiz q WHERE q.quizType = 'ENTRY_TEST' AND (q.courseId = :courseId OR :courseId IS NULL)")
    Optional<Quiz> findEntryTestByCourseId(@Param("courseId") Integer courseId);

    @Query("SELECT q FROM Quiz q WHERE q.quizType = 'ENTRY_TEST'")
    List<Quiz> findAllEntryTests();
}
