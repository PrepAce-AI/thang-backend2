package backend.repository;

import backend.entity.TestSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestSessionRepository extends JpaRepository<TestSession, Integer> {

    // Hai method này là đúng nhất
    Optional<TestSession> findBySessionsIdAndStudentId(Integer sessionsId, Integer studentId);

    List<TestSession> findByStudentIdOrderByStartedAtDesc(Integer studentId);
    List<TestSession> findByStatus(String status);
    List<TestSession> findByQuiz_QuizId(Integer quizId);
}