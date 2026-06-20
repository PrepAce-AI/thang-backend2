package backend.repository;

import backend.entity.InVideoQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InVideoQuestionRepository extends JpaRepository<InVideoQuestion, Integer> {
    List<InVideoQuestion> findByLessonIdOrderByTimestampSecondsAsc(Integer lessonId);
}
