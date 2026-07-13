package backend.repository;

import backend.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Integer> {

    // Tìm kiếm các khóa học theo trạng thái chuỗi hệ thống
    List<Course> findByStatus(String status);

    // Tìm kiếm danh sách khóa học dựa theo ID của giáo viên phụ trách
    List<Course> findByTeacherId(Integer teacherId);
}