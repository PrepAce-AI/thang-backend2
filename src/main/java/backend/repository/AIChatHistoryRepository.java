package backend.repository;

import backend.entity.AIChatHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AIChatHistoryRepository extends JpaRepository<AIChatHistory, Integer> {

    Page<AIChatHistory> findByStudentIdOrderByCreatedAtDesc(Integer studentId, Pageable pageable);

    void deleteByStudentId(Integer studentId);
}
