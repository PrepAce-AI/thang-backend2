package backend.repository;

import backend.entity.PracticeAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PracticeAnswerRepository extends JpaRepository<PracticeAnswer, Integer> {

    /** Lấy toàn bộ câu trả lời của 1 lượt luyện đề, kèm câu hỏi + options (1 query, tránh N+1) */
    @Query("""
        SELECT DISTINCT pa FROM PracticeAnswer pa
        JOIN FETCH pa.question q
        LEFT JOIN FETCH q.options
        WHERE pa.attempt.attemptId = :attemptId
        ORDER BY pa.questionOrder
        """)
    List<PracticeAnswer> findByAttemptIdWithQuestions(@Param("attemptId") Integer attemptId);

    /**
     * Toàn bộ câu ĐÃ CHẤM ĐIỂM của 1 học sinh, xuyên suốt mọi lượt Entry Test/Luyện đề/Thi thử
     * đã nộp bài — nguồn dữ liệu DUY NHẤT cho AI Gap Diagnosis / Adaptive Path / Score Forecast /
     * University Advising. Không dùng dữ liệu giả lập (khác bản cũ nhóm theo quizId % 4).
     */
    @Query("""
        SELECT pa FROM PracticeAnswer pa
        JOIN FETCH pa.question q
        WHERE pa.attempt.studentId = :studentId
          AND pa.attempt.submittedAt IS NOT NULL
          AND pa.isCorrect IS NOT NULL
        """)
    List<PracticeAnswer> findGradedByStudentId(@Param("studentId") Integer studentId);

    @Modifying
    @Transactional
    @Query(
            value = "DELETE FROM PracticeAnswers WHERE question_id = :questionId",
            nativeQuery = true
    )
    void deleteByQuestionId(@Param("questionId") Integer questionId);

    void deleteByAttempt_AttemptId(Integer attemptId);
}
