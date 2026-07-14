package backend.repository;

import backend.entity.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Integer> {
    List<StudentAnswer> findBySessionSessionsId(Integer sessionsId);

    Optional<StudentAnswer> findBySessionSessionsIdAndQuestionQuestionId(Integer sessionsId, Integer questionId);
}
