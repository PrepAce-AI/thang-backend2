package backend.repository;

import backend.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Integer> {

    // Lấy danh sách các môn học không bị ẩn để hiển thị cho Giáo viên chọn khi tạo khóa học
    List<Subject> findByIsHiddenFalse();

    // Tìm các môn học thuộc về một danh mục khối thi cụ thể
    List<Subject> findByCategoryIdAndIsHiddenFalse(Integer categoryId);
}