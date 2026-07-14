package backend.controller;

import backend.entity.Chapter;
import backend.entity.Lesson;
import backend.repository.ChapterRepository;
import backend.repository.CourseRepository;
import backend.repository.LessonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/outlines")
public class OutlineController {

    @Autowired private ChapterRepository chapterRepository;
    @Autowired private LessonRepository lessonRepository;
    @Autowired private CourseRepository courseRepository; // Đảm bảo bạn đã có CourseRepository

    // 1. API: Thêm Chương mới vào Khóa học
    @PostMapping("/courses/{courseId}/chapters")
    public ResponseEntity<?> addChapter(@PathVariable Integer courseId, @RequestBody Map<String, Object> body) {
        var courseOptional = courseRepository.findById(courseId);
        if (courseOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("Không tìm thấy khóa học mã số: " + courseId);
        }

        // Tính toán số thứ tự tự động tăng cho chương mới
        List<Chapter> existingChapters = chapterRepository.findByCourse_CourseIdOrderByOrderAsc(courseId);
        int nextOrder = existingChapters.size() + 1;

        Chapter chapter = new Chapter();
        chapter.setTitle((String) body.get("title"));
        chapter.setOrder(nextOrder);
        chapter.setCourse(courseOptional.get());

        chapterRepository.save(chapter);
        return ResponseEntity.ok(Map.of("message", "Thêm chương thành công"));
    }

    // 2. API: Thêm Bài học mới vào một Chương cụ thể
    @PostMapping("/chapters/{chapterId}/lessons")
    public ResponseEntity<?> addLesson(@PathVariable Integer chapterId, @RequestBody Map<String, Object> body) {
        var chapterOptional = chapterRepository.findById(chapterId);
        if (chapterOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("Không tìm thấy chương học tương ứng!");
        }

        Chapter chapter = chapterOptional.get();
        int nextOrder = (chapter.getLessons() != null) ? chapter.getLessons().size() + 1 : 1;

        Lesson lesson = new Lesson();
        lesson.setTitle((String) body.get("title"));
        lesson.setDescription((String) body.get("description"));
        lesson.setVideoUrl((String) body.get("videoUrl"));
        lesson.setDuration((String) body.get("duration"));
        lesson.setOrder(nextOrder);
        lesson.setChapter(chapter);
        if (body.containsKey("isPreview")) {
            lesson.setIsPreview(Boolean.parseBoolean(String.valueOf(body.get("isPreview"))));
        }

        lessonRepository.save(lesson);
        return ResponseEntity.ok(Map.of("message", "Thêm bài học thành công"));
    }
    // ==============================================================
    // 🛠️ BỔ SUNG CÁC API CẬP NHẬT VÀ XÓA ĐỀ CƯƠNG (CRUD)
    // ==============================================================

    // 3. API: Sửa tên Chương
    @PutMapping("/chapters/{chapterId}")
    public ResponseEntity<?> updateChapter(@PathVariable Integer chapterId, @RequestBody Map<String, Object> body) {
        return chapterRepository.findById(chapterId).map(chapter -> {
            chapter.setTitle((String) body.get("title"));
            if (body.containsKey("order")) {
                chapter.setOrder((Integer) body.get("order"));
            }
            chapterRepository.save(chapter);
            return ResponseEntity.ok(Map.of("message", "Cập nhật chương thành công"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // 4. API: Xóa Chương (Tự động xóa luôn các Lessons bên trong nhờ CascadeType.ALL)
    @DeleteMapping("/chapters/{chapterId}")
    public ResponseEntity<?> deleteChapter(@PathVariable Integer chapterId) {
        if (!chapterRepository.existsById(chapterId)) {
            return ResponseEntity.notFound().build();
        }
        chapterRepository.deleteById(chapterId);
        return ResponseEntity.ok(Map.of("message", "Xóa chương và toàn bộ bài học thành công!"));
    }

    // 5. API: Sửa thông tin Bài học
    @PutMapping("/lessons/{lessonId}")
    public ResponseEntity<?> updateLesson(@PathVariable Integer lessonId, @RequestBody Map<String, Object> body) {
        return lessonRepository.findById(lessonId).map(lesson -> {
            if (body.containsKey("title")) lesson.setTitle((String) body.get("title"));
            if (body.containsKey("description")) lesson.setDescription((String) body.get("description"));
            if (body.containsKey("videoUrl")) lesson.setVideoUrl((String) body.get("videoUrl"));
            if (body.containsKey("duration")) lesson.setDuration((String) body.get("duration"));
            if (body.containsKey("order")) lesson.setOrder((Integer) body.get("order"));
            if (body.containsKey("isPreview")) {
                lesson.setIsPreview(Boolean.parseBoolean(String.valueOf(body.get("isPreview"))));
            }
            lessonRepository.save(lesson);
            return ResponseEntity.ok(Map.of("message", "Cập nhật bài học thành công"));
        }).orElse(ResponseEntity.notFound().build());
    }

    // 6. API: Xóa Bài học
    @DeleteMapping("/lessons/{lessonId}")
    public ResponseEntity<?> deleteLesson(@PathVariable Integer lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            return ResponseEntity.notFound().build();
        }
        lessonRepository.deleteById(lessonId);
        return ResponseEntity.ok(Map.of("message", "Xóa bài học thành công!"));
    }

    @Autowired private backend.repository.LearningMaterialRepository materialRepository;

    // 7. API: Thêm tài liệu vào Bài học
    @PostMapping("/lessons/{lessonId}/materials")
    public ResponseEntity<?> addMaterial(@PathVariable Integer lessonId, @RequestBody Map<String, Object> body) {
        var lessonOptional = lessonRepository.findById(lessonId);
        if (lessonOptional.isEmpty()) {
            return ResponseEntity.badRequest().body("Không tìm thấy bài học");
        }
        backend.entity.LearningMaterial mat = new backend.entity.LearningMaterial();
        mat.setTitle((String) body.get("title"));
        mat.setFileUrl((String) body.get("fileUrl"));
        mat.setLesson(lessonOptional.get());
        return ResponseEntity.ok(materialRepository.save(mat));
    }

    // 8. API: Xóa tài liệu
    @DeleteMapping("/materials/{materialId}")
    public ResponseEntity<?> deleteMaterial(@PathVariable Integer materialId) {
        if (!materialRepository.existsById(materialId)) {
            return ResponseEntity.notFound().build();
        }
        materialRepository.deleteById(materialId);
        return ResponseEntity.ok(Map.of("message", "Xóa tài liệu thành công"));
    }
}