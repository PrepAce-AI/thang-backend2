package backend.repository;

import backend.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Integer> {
    // JpaRepository hỗ trợ sẵn các hàm Save, Delete, FindById cơ bản cho Lesson
}