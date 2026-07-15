package backend.repository;

import backend.entity.FlashQuiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FlashQuizRepository extends JpaRepository<FlashQuiz, Integer> {

    List<FlashQuiz> findByStudentId(Integer studentId);

    List<FlashQuiz> findByChapterId(Integer chapterId);

    Optional<FlashQuiz> findByStudentIdAndChapterId(Integer studentId, Integer chapterId);
}