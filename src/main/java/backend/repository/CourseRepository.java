package backend.repository;

import backend.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {
    // Kế thừa sẵn các hàm tìm kiếm cơ bản
}