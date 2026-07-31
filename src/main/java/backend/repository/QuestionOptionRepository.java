package backend.repository;
import backend.entity.QuestionOption;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface QuestionOptionRepository extends JpaRepository<QuestionOption, Integer> {
    @Modifying
    @Transactional
    @Query(
            value = "DELETE FROM QuestionOptions WHERE question_id = :questionId",
            nativeQuery = true
    )
    void deleteByQuestionId(@Param("questionId") Integer questionId);
}