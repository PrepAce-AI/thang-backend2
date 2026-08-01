package backend.repository;
import backend.entity.Question;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer>{
    @Query("""
SELECT q
FROM Question q
WHERE q.quiz.quizId = :quizId
""")
    List<Question> findByQuizId(@Param("quizId") Integer quizId);

    /**
     * Luyện Đề: lấy ngẫu nhiên :count question_id từ kho câu hỏi của quiz.
     * ORDER BY NEWID() là cách random chuẩn của SQL Server.
     * Chỉ lấy ID (nhẹ), sau đó fetch đầy đủ bằng findWithOptionsByIds — tránh N+1.
     */
    @Query(value = """
            SELECT TOP (:count) question_id
            FROM Questions
            WHERE quiz_id = :quizId
            ORDER BY NEWID()
            """, nativeQuery = true)
    List<Integer> findRandomQuestionIds(@Param("quizId") Integer quizId, @Param("count") int count);

    /** Fetch câu hỏi + toàn bộ options trong 1 query */
    @Query("""
            SELECT DISTINCT q
            FROM Question q
            LEFT JOIN FETCH q.options
            WHERE q.questionId IN :ids
            """)
    List<Question> findWithOptionsByIds(@Param("ids") List<Integer> ids);

    @Modifying
    @Transactional
    @Query("""
       DELETE FROM Question q
       WHERE q.quiz.quizId = :quizId
       """)
    void deleteByQuizId(Integer quizId);

    /** Đếm số câu trong kho của 1 quiz */
    long countByQuiz_QuizId(Integer quizId);
}
