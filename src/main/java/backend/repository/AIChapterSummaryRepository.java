package backend.repository;

import backend.entity.AIChapterSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AIChapterSummaryRepository
        extends JpaRepository<AIChapterSummary, Integer> {

    Optional<AIChapterSummary> findFirstByStudent_IdAndChapter_Id(
            Integer studentId,
            Integer chapterId
    );
}