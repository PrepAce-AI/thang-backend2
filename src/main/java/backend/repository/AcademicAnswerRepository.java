package backend.repository;

import backend.entity.AcademicAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AcademicAnswerRepository extends JpaRepository<AcademicAnswer, Integer> {
    List<AcademicAnswer> findByQuestionIdOrderByCreatedAtAsc(Integer questionId);
}
