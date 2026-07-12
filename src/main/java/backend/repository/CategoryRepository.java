package backend.repository;

import backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    // Chỉ tìm các danh mục không bị ẩn dành cho luồng học sinh mới và giáo viên tạo khóa
    List<Category> findByIsHiddenFalse();

    // Kiểm tra trùng tên danh mục (Exception E-01)
    boolean existsByCategoryNameAndIsHiddenFalse(String categoryName);
}