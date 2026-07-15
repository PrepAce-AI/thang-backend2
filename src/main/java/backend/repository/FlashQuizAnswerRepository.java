package backend.repository;

import backend.entity.FlashQuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlashQuizAnswerRepository extends JpaRepository<FlashQuizAnswer, Integer> {

    List<FlashQuizAnswer> findByFlashQuizFlashQuizId(Integer flashQuizId);

}