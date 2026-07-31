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

    // 🔥 SỬA CHUẨN: Lấy tất cả và giữ lại các danh mục KHÔNG ẨN (kể cả giá trị NULL)
    @GetMapping("/public/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        List<Category> allCategories = categoryRepository.findAll();
        List<Category> visibleCategories = allCategories.stream()
                .filter(c -> c.getIsHidden() == null || !c.getIsHidden())
                .toList();

        return ResponseEntity.ok(visibleCategories);
    }
}