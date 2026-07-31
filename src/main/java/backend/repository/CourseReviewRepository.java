package backend.repository;

import backend.entity.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CourseReviewRepository extends JpaRepository<CourseReview, Integer> {
    // Lấy danh sách đánh giá của một khóa học (mới nhất lên đầu)
    List<CourseReview> findByCourse_CourseIdOrderByCreatedAtDesc(Integer courseId);
}