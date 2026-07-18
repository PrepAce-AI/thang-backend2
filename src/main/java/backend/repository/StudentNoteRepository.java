package backend.repository;

import backend.entity.StudentNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentNoteRepository extends JpaRepository<StudentNote, Integer> {
    List<StudentNote> findByLessonIdAndUserIdOrderByTimestampSecondsAsc(Integer lessonId, Integer userId);
}
