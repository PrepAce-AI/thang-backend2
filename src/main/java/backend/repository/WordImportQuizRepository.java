package backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import backend.entity.Quiz;

/**
 * Repository phụ cho WordImportService.
 * Dùng native query để INSERT Quiz với course_id = NULL
 * (tránh JPA validation nullable=false của @JoinColumn).
 */
@Repository
public interface WordImportQuizRepository extends JpaRepository<Quiz, Integer> {

    /**
     * Tạo Quiz mới với course_id = NULL (đề độc lập, không gắn khóa học).
     * Trả về quiz_id vừa insert qua SCOPE_IDENTITY().
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO Quizzes (quiz_title, quiz_type, subject, duration_minutes, is_entry_test, created_at)
        VALUES (:title, 'PRACTICE', :subject, :duration, 0, GETDATE())
        """, nativeQuery = true)
    void insertQuizNoCourse(
            @Param("title")    String title,
            @Param("subject")  String subject,
            @Param("duration") int duration
    );

    /**
     * Tạo Quiz mới với course_id cụ thể.
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO Quizzes (quiz_title, quiz_type, subject, duration_minutes, is_entry_test, course_id, created_at)
        VALUES (:title, 'PRACTICE', :subject, :duration, 0, :courseId, GETDATE())
        """, nativeQuery = true)
    void insertQuizWithCourse(
            @Param("title")    String title,
            @Param("subject")  String subject,
            @Param("duration") int duration,
            @Param("courseId") int courseId
    );

    /**
     * Lấy quiz_id của đề vừa tạo (mới nhất theo title).
     */
    @Query(value = "SELECT TOP 1 quiz_id FROM Quizzes WHERE quiz_title = :title ORDER BY quiz_id DESC",
           nativeQuery = true)
    Integer findLatestQuizIdByTitle(@Param("title") String title);
}
