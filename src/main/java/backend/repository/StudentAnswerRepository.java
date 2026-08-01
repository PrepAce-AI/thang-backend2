package backend.repository;

import backend.entity.StudentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Integer> {
    List<StudentAnswer> findBySessionSessionsId(Integer sessionsId);

    Optional<StudentAnswer> findBySessionSessionsIdAndQuestionQuestionId(Integer sessionsId, Integer questionId);

    @Modifying
    @Transactional
    @Query(
            value = "DELETE FROM StudentAnswers WHERE question_id = :questionId",
            nativeQuery = true
    )
    void deleteByQuestionId(@Param("questionId") Integer questionId);

    void deleteBySession_SessionsId(Integer sessionsId);
}
