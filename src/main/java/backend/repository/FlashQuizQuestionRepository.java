package backend.repository;

import backend.entity.FlashQuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlashQuizQuestionRepository extends JpaRepository<FlashQuizQuestion, Integer> {

    List<FlashQuizQuestion> findByFlashQuizFlashQuizId(Integer flashQuizId);

}