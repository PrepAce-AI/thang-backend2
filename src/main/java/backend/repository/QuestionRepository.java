package backend.repository;

import backend.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {

    /** Fetch questions + options trong 1 query — tránh N+1 */
    @Query("SELECT DISTINCT q FROM Question q LEFT JOIN FETCH q.options WHERE q.quiz.quizId = :quizId")
    List<Question> findByQuizIdWithOptions(@Param("quizId") Integer quizId);
}
