package backend.repository;
import backend.entity.Quiz;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Integer>{

    @Query("SELECT q FROM Quiz q WHERE q.course.id = :courseId")
    List<Quiz> findByCourseId(@Param("courseId") Integer courseId);
}
