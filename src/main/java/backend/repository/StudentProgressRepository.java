package backend.repository;

import backend.entity.StudentProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentProgressRepository extends JpaRepository<StudentProgress, Integer> {
    Optional<StudentProgress> findByUserIdAndLessonId(int userId, int lessonId);
    List<StudentProgress> findByLessonId(int lessonId);
    List<StudentProgress> findByUserId(int userId);

    @Query("SELECT sp.lesson.id FROM StudentProgress sp WHERE sp.user.id = :userId AND sp.isCompleted = true AND sp.lesson.chapter.course.id = :courseId")
    List<Integer> findCompletedLessonIdsByUserIdAndCourseId(@Param("userId") int userId, @Param("courseId") int courseId);
}
