package backend.controller;

import backend.entity.Category;
import backend.repository.CategoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // 🔥 ĐÃ CẬP NHẬT: Dùng đúng hàm lọc danh mục không bị ẩn của bạn
    @GetMapping("/public/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryRepository.findByIsHiddenFalse());
    }
}