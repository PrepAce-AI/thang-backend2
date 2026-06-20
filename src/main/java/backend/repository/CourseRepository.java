package backend.repository;

import backend.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {
    List<Course> findByIsPublishedTrue();
    List<Course> findByTeacherId(Integer teacherId);
}
