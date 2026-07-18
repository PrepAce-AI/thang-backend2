package backend.controller;

import backend.entity.Question;
import backend.entity.Quiz;
import backend.entity.QuestionOption;
import backend.repository.QuestionRepository;
import backend.repository.QuizRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class QuestionController {

    private final QuestionRepository questionRepository;
    private final QuizRepository quizRepository;

    public QuestionController(QuestionRepository questionRepository, QuizRepository quizRepository) {
        this.questionRepository = questionRepository;
        this.quizRepository = quizRepository;
    }

    // 🔥 API THÊM CÂU HỎI VÀO ĐỀ: Đảm bảo mapping khóa ngoại 2 chiều đầy đủ
    @PostMapping("/quiz/{quizId}")
    public ResponseEntity<?> createQuestion(@PathVariable Integer quizId, @RequestBody Question question) {
        try {
            // 1. Tìm Quiz đích để liên kết khóa ngoại quiz_id
            Quiz quiz = quizRepository.findById(quizId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đề thi liên kết"));
            question.setQuiz(quiz);
            question.setCreatedAt(new java.util.Date());

            // 2. Thiết lập liên kết ngược từ từng Option về lại Question (Tránh lỗi null question_id ở bảng QuestionOptions)
            if (question.getOptions() != null) {
                for (QuestionOption option : question.getOptions()) {
                    option.setQuestion(question); // Gán thực thể cha vào thực thể con
                }
            }

            // 3. Lưu xuống Database (Cơ chế CascadeType.ALL sẽ tự động lưu cả bảng QuestionOptions đi kèm)
            Question savedQuestion = questionRepository.save(question);
            return ResponseEntity.ok(savedQuestion);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi Backend khi thêm câu hỏi: " + e.getMessage());
        }
    }
}