package backend.repository;

import backend.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Integer> {

    List<QuizAttempt> findByStudentIdOrderBySubmittedAtDesc(Integer studentId);

    /** Kiểm tra student đã làm entry test chưa */
    @Query("""
        SELECT a FROM QuizAttempt a
        JOIN a.quiz q
        WHERE a.studentId = :studentId
          AND q.quizType = 'ENTRY_TEST'
        ORDER BY a.submittedAt DESC
        """)
    List<QuizAttempt> findEntryTestAttemptsByStudent(@Param("studentId") Integer studentId);

    /** Lấy lịch sử attempt theo quiz */
    @Query("SELECT a FROM QuizAttempt a WHERE a.quiz.quizId = :quizId AND a.studentId = :studentId ORDER BY a.submittedAt DESC")
    List<QuizAttempt> findByQuizIdAndStudentId(@Param("quizId") Integer quizId, @Param("studentId") Integer studentId);

    /** Điểm trung bình của student (dùng cho AI Score Forecasting) */
    @Query("SELECT AVG(a.score) FROM QuizAttempt a WHERE a.studentId = :studentId AND a.submittedAt IS NOT NULL")
    Optional<Double> findAverageScoreByStudentId(@Param("studentId") Integer studentId);
    List<QuizAttempt> findByStatus(String status);
    List<QuizAttempt> findByQuiz_QuizId(Integer quizId);
}
