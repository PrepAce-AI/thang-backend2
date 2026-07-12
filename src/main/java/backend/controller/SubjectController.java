package backend.controller;

import backend.entity.Subject;
import backend.repository.SubjectRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class SubjectController {

    private final SubjectRepository subjectRepository;

    public SubjectController(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    // API phục vụ Frontend lấy danh sách môn học đang hoạt động
    @GetMapping("/public/subjects")
    public ResponseEntity<List<Subject>> getActiveSubjects() {
        // Hàm này gọi từ SubjectRepository ta đã tạo ở bước trước
        return ResponseEntity.ok(subjectRepository.findByIsHiddenFalse());
    }
}